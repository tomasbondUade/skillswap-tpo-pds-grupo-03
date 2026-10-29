package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.model.BookingData;
import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.BookingData.CancelCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RejectCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RescheduleCommand;
import ar.edu.uade.skillswap.application.port.in.BookingUseCases;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.DomainEventPublisher;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.domain.policy.RefundPolicy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates the booking lifecycle.
 * Booking, publication and wallet changes share the same transaction.
 */
public final class BookingApplicationService implements BookingUseCases {

    private final BookingRepository bookings;
    private final PublicationRepository publications;
    private final WalletRepository wallets;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;
    private final RefundPolicy refundPolicy;
    private final DomainEventPublisher events;

    public BookingApplicationService(
            BookingRepository bookings,
            PublicationRepository publications,
            WalletRepository wallets,
            CurrentUserProvider currentUser,
            TransactionManager transactions,
            RefundPolicy refundPolicy,
            DomainEventPublisher events) {

        this.bookings = Objects.requireNonNull(bookings);
        this.publications = Objects.requireNonNull(publications);
        this.wallets = Objects.requireNonNull(wallets);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
        this.refundPolicy = Objects.requireNonNull(refundPolicy);
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public List<BookingView> listMyBookings() {
        String actor = UseCaseSupport.currentUserId(currentUser);

        return bookings.findByParticipantUserId(actor).stream()
                .map(BookingApplicationService::toView)
                .toList();
    }

    @Override
    public BookingView request(RequestCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la solicitud");

        String actor = UseCaseSupport.currentUserId(currentUser);
        String publicationId = UseCaseSupport.requiredId(
                command.publicationId(), "publicación");

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Publication publication =
                            findPublicationForUpdate(publicationId);

                    Booking booking = Booking.request(
                            publication,
                            actor,
                            command.clientMessage(),
                            command.objective(),
                            command.slotId(),
                            publication.basePrice());

                    publications.save(publication);
                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.requested", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView accept(BookingCommand command) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = bookingId(command);

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);

                    // Validar permisos y estado antes de reservar un cupo.
                    booking.assertCanAccept(actor);

                    Publication publication =
                            findPublicationForUpdate(booking.publicationId());

                    // El bloqueo de la publicación protege el último cupo.
                    publication.reserve(booking.slotId());
                    booking.accept(actor);

                    publications.save(publication);
                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.accepted", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView reject(RejectCommand command) {
        Objects.requireNonNull(command, "Faltan los datos del rechazo");

        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = UseCaseSupport.requiredId(
                command.bookingId(), "contratación");

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);
                    booking.reject(actor, command.reason());

                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.rejected", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView pay(BookingCommand command) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = bookingId(command);

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);

                    // Validar estado y comprador antes de modificar las billeteras.
                    booking.assertPaymentAllowed(actor);

                    List<Wallet> lockedWallets =
                            findWalletsForUpdateInOrder(
                                    booking.buyerUserId(),
                                    booking.providerUserId());

                    Wallet buyer = findWalletByUserId(
                            lockedWallets, booking.buyerUserId());

                    Wallet provider = findWalletByUserId(
                            lockedWallets, booking.providerUserId());

                    int amount = booking.price().amount();

                    buyer.debit(
                            amount,
                            "Pago de contratación " + booking.id());

                    provider.credit(
                            amount,
                            "Cobro de contratación " + booking.id());

                    booking.confirmPayment(actor);

                    wallets.save(buyer);
                    wallets.save(provider);

                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.paid", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView cancel(CancelCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la cancelación");

        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = UseCaseSupport.requiredId(
                command.bookingId(), "contratación");

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);
                    BookingStatus previousStatus = booking.status();

                    Publication publication =
                            findPublicationForUpdate(booking.publicationId());

                    boolean wasPaid = booking.paidAt() != null;
                    SkillCoins refund = SkillCoins.zero();

                    if (wasPaid) {
                        LocalDateTime now = LocalDateTime.now();

                        LocalDateTime startsAt = publication.availability()
                                .stream()
                                .filter(slot ->
                                        slot.id().equals(booking.slotId()))
                                .map(slot -> slot.startsAt())
                                .findFirst()
                                .orElseThrow(() -> new ApplicationException(
                                        ApplicationErrorCode.INVALID_STATE,
                                        "No se encontró la franja de la contratación"));

                        refund = refundPolicy.refundFor(
                                booking, now, startsAt);
                    }

                    // Booking.cancel valida participantes y estados permitidos.
                    booking.cancel(actor, command.reason(), refund);

                    if (previousStatus == BookingStatus.ACCEPTED
                            || previousStatus == BookingStatus.CONFIRMED) {

                        publication.releaseReservation(booking.slotId());
                    }

                    if (wasPaid && refund.amount() > 0) {

                        List<Wallet> lockedWallets =
                                findWalletsForUpdateInOrder(
                                        booking.providerUserId(),
                                        booking.buyerUserId());

                        Wallet provider = findWalletByUserId(
                                lockedWallets, booking.providerUserId());

                        Wallet buyer = findWalletByUserId(
                                lockedWallets, booking.buyerUserId());

                        // Si el proveedor no tiene saldo suficiente, la operación
                        // debe fallar y la transacción debe revertir los cambios.
                        provider.debit(
                                refund.amount(),
                                "Reintegro de contratación " + booking.id());

                        buyer.credit(
                                refund.amount(),
                                "Reintegro de contratación " + booking.id());

                        wallets.save(provider);
                        wallets.save(buyer);
                    }

                    publications.save(publication);

                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.cancelled", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView proposeReschedule(RescheduleCommand command) {
        Objects.requireNonNull(
                command, "Faltan los datos de la reprogramación");

        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = UseCaseSupport.requiredId(
                command.bookingId(), "contratación");

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);

                    booking.proposeReschedule(actor, command.newSlotId());

                    Booking saved = bookings.save(booking);

                    publishAfterCommit(
                            "booking.reschedule-proposed", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView acceptReschedule(BookingCommand command) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = bookingId(command);

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);
                    String proposedSlotId = booking.proposedSlotId();

                    if (proposedSlotId == null) {
                        throw new IllegalStateException(
                                "No hay una reprogramación propuesta");
                    }

                    boolean otherAlreadyAccepted =
                            actor.equals(booking.buyerUserId())
                                    ? booking.providerAcceptedReschedule()
                                    : booking.buyerAcceptedReschedule();

                    Publication publication = null;
                    String oldSlotId = booking.slotId();

                    if (otherAlreadyAccepted) {
                        publication = findPublicationForUpdate(
                                booking.publicationId());

                        // Reservar el nuevo cupo antes de liberar el anterior.
                        publication.reserve(proposedSlotId);
                    }

                    boolean completed = booking.acceptReschedule(actor);

                    if (completed) {
                        publication.releaseReservation(oldSlotId);
                        publications.save(publication);
                    }

                    Booking saved = bookings.save(booking);

                    publishAfterCommit(
                            completed ? "booking.rescheduled" : "booking.reschedule-accepted", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView start(BookingCommand command) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = bookingId(command);

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);
                    booking.start(actor);

                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.started", saved, actor);

                    return toView(saved);
                }));
    }

    @Override
    public BookingView complete(BookingCommand command) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = bookingId(command);

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    Booking booking = findBookingForUpdate(id);
                    booking.complete(actor);

                    Booking saved = bookings.save(booking);

                    publishAfterCommit("booking.completed", saved, actor);

                    return toView(saved);
                }));
    }

    private Booking findBookingForUpdate(String id) {
        return bookings.findByIdForUpdate(id)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.NOT_FOUND,
                        "No existe la contratación indicada"));
    }

    private Publication findPublicationForUpdate(String id) {
        return publications.findByIdForUpdate(id)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.NOT_FOUND,
                        "No existe la publicación indicada"));
    }

    private Wallet findWalletForUpdate(String userId) {
        return wallets.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.NOT_FOUND,
                        "No se encontró la billetera del usuario"));
    }

    /**
     * Bloquea las dos billeteras por identificador de usuario ascendente.
     * Así pay() y cancel() no adquieren los bloqueos en orden opuesto.
     */
    private List<Wallet> findWalletsForUpdateInOrder(
            String firstUserId,
            String secondUserId) {

        if (firstUserId.equals(secondUserId)) {
            throw new ApplicationException(
                    ApplicationErrorCode.INVALID_STATE,
                    "El comprador y el proveedor no pueden ser el mismo usuario");
        }

        List<String> userIds = new ArrayList<>(
                List.of(firstUserId, secondUserId));

        userIds.sort(String::compareTo);

        Wallet firstWallet = findWalletForUpdate(userIds.get(0));
        Wallet secondWallet = findWalletForUpdate(userIds.get(1));

        return List.of(firstWallet, secondWallet);
    }

    private static Wallet findWalletByUserId(
            List<Wallet> lockedWallets,
            String userId) {

        return lockedWallets.stream()
                .filter(wallet -> wallet.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la billetera bloqueada"));
    }

    private static String bookingId(BookingCommand command) {
        Objects.requireNonNull(
                command, "Faltan los datos de la contratación");

        return UseCaseSupport.requiredId(
                command.bookingId(), "contratación");
    }

    private void publishAfterCommit(
            String name, Booking booking, String actor) {

        DomainEvent event = new DomainEvent(
                name,
                "Booking",
                booking.id(),
                actor,
                LocalDateTime.now(),
                Map.of(
                        "publicationId", booking.publicationId(),
                        "buyerUserId", booking.buyerUserId(),
                        "providerUserId", booking.providerUserId(),
                        "status", booking.status().name()));

        transactions.afterCommit(() -> events.publish(event));
    }

    private static BookingView toView(Booking booking) {
        return new BookingView(
                booking.id(),
                booking.publicationId(),
                booking.buyerUserId(),
                booking.providerUserId(),
                booking.objective(),
                booking.clientMessage(),
                booking.slotId(),
                booking.price().amount(),
                booking.refund().amount(),
                booking.status(),
                booking.createdAt(),
                booking.paidAt());
    }
}
