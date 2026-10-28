package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.mapper.BookingApplicationMapper;
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
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.policy.RefundPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Coordinates the booking lifecycle and delegates the rules to the domain aggregate. */
public final class BookingApplicationService implements BookingUseCases {
    public static final String ESCROW_WALLET_ID = "system-escrow";

    private record RescheduleResult(BookingView booking, boolean moved) {}

    private final PublicationRepository publications;
    private final BookingRepository bookings;
    private final WalletRepository wallets;
    private final RefundPolicy refundPolicy;
    private final DomainEventPublisher events;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;

    public BookingApplicationService(PublicationRepository publications, BookingRepository bookings,
                                    WalletRepository wallets, RefundPolicy refundPolicy,
                                    CurrentUserProvider currentUser, TransactionManager transactions,
                                    DomainEventPublisher events) {
        this.publications = Objects.requireNonNull(publications);
        this.bookings = Objects.requireNonNull(bookings);
        this.wallets = Objects.requireNonNull(wallets);
        this.refundPolicy = Objects.requireNonNull(refundPolicy);
        this.events = Objects.requireNonNull(events);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public List<BookingView> listMyBookings() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        return bookings.findByParticipantUserId(actor).stream()
                .map(BookingApplicationMapper::toView)
                .toList();
    }

    @Override
    public BookingView request(RequestCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la contratación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String publicationId = UseCaseSupport.requiredId(command.publicationId(), "publicación");
        BookingView requested = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Publication publication = publications.findByIdForUpdate(publicationId)
                    .orElseThrow(() -> notFound("No existe la publicación indicada"));
                Booking booking = Booking.request(publication, actor, command.clientMessage(), command.objective(),
                    command.slotId(), publication.basePrice());
            publications.save(publication);
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.requested", requested, actor);
    }

    @Override
    public BookingView accept(BookingCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la contratación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView accepted = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> notFound("No existe la contratación indicada"));
            booking.assertCanAccept(actor);
            Publication publication = publications.findByIdForUpdate(booking.publicationId())
                .orElseThrow(() -> notFound("No existe la publicación indicada"));
            publication.reserve(booking.slotId());
            booking.accept(actor);
            publications.save(publication);
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.accepted", accepted, actor);
    }

    @Override
    public BookingView reject(RejectCommand command) {
        Objects.requireNonNull(command, "Faltan los datos del rechazo");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView rejected = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            booking.reject(actor, command.reason());
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.rejected", rejected, actor);
    }

    @Override
    public BookingView pay(BookingCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la contratación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView paid = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            booking.assertPaymentAllowed(actor);
            var wallet = wallets.findByUserIdForUpdate(actor)
                    .orElseThrow(() -> notFound("No existe una billetera para el usuario autenticado"));
            var escrow = wallets.findByUserIdForUpdate(ESCROW_WALLET_ID)
                    .orElseThrow(() -> notFound("No existe la billetera de custodia"));
            escrow.balance().plus(booking.price());
            wallet.debit(booking.price().amount(), "Pago de contratación " + booking.id());
            escrow.credit(booking.price().amount(), "Custodia de contratación " + booking.id());
            booking.confirmPayment(actor);
            wallets.save(wallet);
            wallets.save(escrow);
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
            return publishEvent("booking.paid", paid, actor);
    }

    @Override
    public BookingView cancel(CancelCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la cancelación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView canceled = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            BookingStatus previousStatus = booking.status();
            Publication publication = null;
            SkillCoins refund = SkillCoins.zero();
            var buyerWallet = booking.paidAt() == null ? null : wallets.findByUserIdForUpdate(booking.buyerUserId())
                    .orElseThrow(() -> notFound("No existe la billetera del cliente"));
            if (booking.paidAt() != null) {
                publication = publications.findByIdForUpdate(booking.publicationId())
                        .orElseThrow(() -> notFound("No existe la publicación de la contratación"));
                LocalDateTime sessionStartsAt = publication.availability().stream()
                        .filter(slot -> slot.id().equals(booking.slotId()))
                        .map(slot -> slot.startsAt())
                        .findFirst()
                        .orElseThrow(() -> notFound("No existe el horario de la contratación"));
                refund = refundPolicy.refundFor(booking, LocalDateTime.now(), sessionStartsAt);
                if (refund.amount() > 0) {
                    buyerWallet.balance().plus(refund);
                }
            }
                var escrow = booking.paidAt() == null ? null : wallets.findByUserIdForUpdate(ESCROW_WALLET_ID)
                    .orElseThrow(() -> notFound("No existe la billetera de custodia"));
                int providerAmount = booking.paidAt() == null ? 0 : booking.price().amount() - refund.amount();
                var providerWallet = providerAmount == 0 ? null : wallets.findByUserIdForUpdate(booking.providerUserId())
                    .orElseThrow(() -> notFound("No existe la billetera del proveedor"));
                if (escrow != null) {
                escrow.balance().minus(new SkillCoins(booking.price().amount()));
                }
                if (providerWallet != null) {
                providerWallet.balance().plus(new SkillCoins(providerAmount));
            }
            boolean reservationExists = previousStatus == BookingStatus.ACCEPTED
                    || previousStatus == BookingStatus.CONFIRMED || previousStatus == BookingStatus.IN_PROGRESS;
            if (reservationExists) {
                if (publication == null) {
                    publication = publications.findByIdForUpdate(booking.publicationId())
                            .orElseThrow(() -> notFound("No existe la publicación de la contratación"));
                }
                if (findSlot(publication, booking.slotId()).reserved() < 1) {
                    throw new IllegalStateException("La contratación no tiene un cupo reservado para liberar");
                }
            }
            booking.cancel(actor, command.reason(), refund);
            if (reservationExists) {
                publication.releaseReservation(booking.slotId());
                publications.save(publication);
            }
            if (booking.paidAt() != null) {
                if (refund.amount() > 0) {
                    buyerWallet.credit(refund.amount(), "Reintegro de contratación " + booking.id());
                    wallets.save(buyerWallet);
                    escrow.debit(refund.amount(), "Reintegro de contratación " + booking.id());
                }
                if (providerAmount > 0) {
                    providerWallet.credit(providerAmount, "Liquidación de contratación cancelada " + booking.id());
                    wallets.save(providerWallet);
                    escrow.debit(providerAmount, "Liquidación de contratación cancelada " + booking.id());
                }
                wallets.save(escrow);
            }
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.cancelled", canceled, actor);
    }

    @Override
    public BookingView proposeReschedule(RescheduleCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la reprogramación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView proposed = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
                booking.assertCanProposeReschedule(actor, command.newSlotId());
            Publication publication = publications.findByIdForUpdate(booking.publicationId())
                    .orElseThrow(() -> notFound("No existe la publicación indicada"));
            requireAvailableSlot(publication, command.newSlotId());
            booking.proposeReschedule(actor, command.newSlotId());
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.reschedule-proposed", proposed, actor);
    }

    @Override
    public BookingView acceptReschedule(BookingCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la reprogramación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        RescheduleResult result = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            booking.assertCanAcceptReschedule(actor);
            boolean completesReschedule = booking.rescheduleWillBeAcceptedBy(actor);
            String previousSlotId = booking.slotId();
            Publication publication = publications.findByIdForUpdate(booking.publicationId())
                    .orElseThrow(() -> notFound("No existe la publicación indicada"));
            AvailabilitySlot targetSlot = findSlot(publication, booking.proposedSlotId());
            AvailabilitySlot previousSlot = findSlot(publication, booking.slotId());
            if (completesReschedule) {
                if (targetSlot.remaining() <= 0) {
                    throw new IllegalStateException("La nueva franja ya no tiene cupo");
                }
                if (previousSlot.reserved() < 1) {
                    throw new IllegalStateException("La contratación no tiene un cupo reservado para transferir");
                }
            }
            boolean rescheduled = booking.acceptReschedule(actor);
            if (rescheduled) {
                publication.releaseReservation(previousSlotId);
                publication.reserve(targetSlot.id());
                publications.save(publication);
            }
            return new RescheduleResult(BookingApplicationMapper.toView(bookings.save(booking)), rescheduled);
        }));
        String eventName = result.moved() ? "booking.rescheduled" : "booking.reschedule-accepted";
        return publishEvent(eventName, result.booking(), actor);
    }

    @Override
    public BookingView start(BookingCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la contratación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView started = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            booking.start(actor);
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.started", started, actor);
    }

    @Override
    public BookingView complete(BookingCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la contratación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        BookingView completed = transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            var providerWallet = wallets.findByUserIdForUpdate(booking.providerUserId())
                    .orElseThrow(() -> notFound("No existe la billetera del proveedor"));
            var escrow = wallets.findByUserIdForUpdate(ESCROW_WALLET_ID)
                    .orElseThrow(() -> notFound("No existe la billetera de custodia"));
            providerWallet.balance().plus(booking.price());
            escrow.balance().minus(booking.price());
            booking.complete(actor);
            escrow.debit(booking.price().amount(), "Liberación de contratación " + booking.id());
            providerWallet.credit(booking.price().amount(), "Ingreso de contratación " + booking.id());
            wallets.save(escrow);
            wallets.save(providerWallet);
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
        return publishEvent("booking.completed", completed, actor);
    }

    private BookingView mutateBooking(BookingCommand command, BookingMutation operation) {
        Objects.requireNonNull(command, "Faltan los datos de la contratación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Booking booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            operation.apply(booking, actor);
            return BookingApplicationMapper.toView(bookings.save(booking));
        }));
    }

    private static ApplicationException notFound(String message) {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, message);
    }

    private static AvailabilitySlot findSlot(Publication publication, String slotId) {
        return publication.availability().stream()
                .filter(slot -> slot.id().equals(slotId))
                .findFirst()
                .orElseThrow(() -> notFound("No existe el horario de la publicación"));
    }

    private BookingView publishEvent(String name, BookingView booking, String actor) {
        events.publish(new DomainEvent(name, "Booking", booking.id(), actor, LocalDateTime.now(), Map.of(
                "buyerUserId", booking.buyerUserId(),
                "providerUserId", booking.providerUserId(),
                "publicationId", booking.publicationId(),
                "slotId", booking.slotId(),
                "detail", "La contratación " + booking.id() + " cambió a " + booking.status() + ".")));
        return booking;
    }

    private static void requireAvailableSlot(Publication publication, String slotId) {
        if (findSlot(publication, slotId).remaining() <= 0) {
            throw new IllegalStateException("La franja ya no tiene cupo");
        }
    }

    @FunctionalInterface
    private interface BookingMutation {
        void apply(Booking booking, String actor);
    }
}
