package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.CancelCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RejectCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RescheduleCommand;
import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.domain.policy.StandardRefundPolicy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BookingApplicationServiceTest {
    @Test
    void requestsAcceptsPaysStartsAndCompletesABooking() {
        Publication publication = Publication.builder("provider")
                .title("Java avanzado")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .availability(List.of(slot()))
                .build();
        publication.publish("provider");

        var publications = new InMemoryPublications(publication);
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("buyer", 250));
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        List<DomainEvent> events = new ArrayList<>();
        AtomicReference<String> currentUser = new AtomicReference<>("buyer");
        var service = new BookingApplicationService(publications, bookings, wallets,
            new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), events::add);

        var requested = service.request(new RequestCommand(publication.id(), "Objetivo", publication.availability().get(0).id()));
        assertEquals("buyer", requested.buyerUserId());
        assertEquals("provider", requested.providerUserId());
        assertEquals(BookingStatus.PENDING, requested.status());

        currentUser.set("provider");
        var accepted = service.accept(new BookingCommand(requested.id()));
        assertEquals(BookingStatus.ACCEPTED, accepted.status());

        currentUser.set("buyer");
        var paid = service.pay(new BookingCommand(requested.id()));
        assertEquals(BookingStatus.CONFIRMED, paid.status());
        assertEquals(150, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(100, wallets.findByUserId(BookingApplicationService.ESCROW_WALLET_ID).orElseThrow().balance().amount());
        assertThrows(RuntimeException.class, () -> service.pay(new BookingCommand(requested.id())));
        assertEquals(150, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(3, events.size());

        currentUser.set("provider");
        var started = service.start(new BookingCommand(requested.id()));
        assertEquals(BookingStatus.IN_PROGRESS, started.status());

        currentUser.set("buyer");
        var completed = service.complete(new BookingCommand(requested.id()));
        assertEquals(BookingStatus.COMPLETED, completed.status());
        assertEquals(100, wallets.findByUserId("provider").orElseThrow().balance().amount());
        assertEquals(0, wallets.findByUserId(BookingApplicationService.ESCROW_WALLET_ID).orElseThrow().balance().amount());
        assertEquals(List.of("booking.requested", "booking.accepted", "booking.paid",
            "booking.started", "booking.completed"), events.stream().map(DomainEvent::name).toList());
    }

    @Test
    void refundsPaidBookingUsingTheConfiguredPolicy() {
        Publication publication = Publication.builder("provider")
                .title("Java avanzado")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .availability(List.of(slot()))
                .build();
        publication.publish("provider");

        var publications = new InMemoryPublications(publication);
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("buyer", 250));
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        AtomicReference<String> currentUser = new AtomicReference<>("buyer");
        var service = new BookingApplicationService(publications, bookings, wallets,
            new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), event -> {});

        var requested = service.request(new RequestCommand(publication.id(), "Objetivo", publication.availability().get(0).id()));
        currentUser.set("provider");
        service.accept(new BookingCommand(requested.id()));
        currentUser.set("buyer");
        service.pay(new BookingCommand(requested.id()));
        var canceled = service.cancel(new CancelCommand(requested.id(), "Cambio de planes"));

        assertEquals(100, canceled.refundInSkillCoins());
        assertEquals(250, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(0, wallets.findByUserId(BookingApplicationService.ESCROW_WALLET_ID).orElseThrow().balance().amount());
        assertEquals(0, publication.availability().get(0).reserved());
        int buyerMovementCount = wallets.findByUserId("buyer").orElseThrow().movements().size();
        assertThrows(RuntimeException.class,
            () -> service.cancel(new CancelCommand(requested.id(), "Cancelación repetida")));
        assertEquals(buyerMovementCount, wallets.findByUserId("buyer").orElseThrow().movements().size());
    }

    @Test
    void paysTheNonRefundedEscrowRemainderToProviderOnPartialRefund() {
        LocalDateTime startsAt = LocalDateTime.now().plusHours(36);
        AvailabilitySlot slot = AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2);
        Publication publication = Publication.builder("provider")
                .title("Java avanzado")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .availability(List.of(slot))
                .build();
        publication.publish("provider");
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("buyer", 250));
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        AtomicReference<String> currentUser = new AtomicReference<>("buyer");
        var service = new BookingApplicationService(new InMemoryPublications(publication), bookings, wallets,
                new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), event -> {});

        var requested = service.request(new RequestCommand(publication.id(), "Objetivo", slot.id()));
        currentUser.set("provider");
        service.accept(new BookingCommand(requested.id()));
        currentUser.set("buyer");
        service.pay(new BookingCommand(requested.id()));
        var canceled = service.cancel(new CancelCommand(requested.id(), "Cambio de planes"));

        assertEquals(50, canceled.refundInSkillCoins());
        assertEquals(200, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(50, wallets.findByUserId("provider").orElseThrow().balance().amount());
        assertEquals(0, wallets.findByUserId(BookingApplicationService.ESCROW_WALLET_ID).orElseThrow().balance().amount());
    }

    @Test
    void acceptReservesCapacityAndLeavesOverflowingRequestPending() {
        Publication publication = Publication.builder("provider")
                .title("Java avanzado")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .availability(List.of(slot()))
                .build();
        publication.publish("provider");

        var publications = new InMemoryPublications(publication);
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        AtomicReference<String> currentUser = new AtomicReference<>("buyer-1");
        var service = new BookingApplicationService(publications, bookings, wallets,
            new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), event -> {});
        String slotId = publication.availability().get(0).id();
        var first = service.request(new RequestCommand(publication.id(), "Objetivo", slotId));
        currentUser.set("buyer-2");
        var second = service.request(new RequestCommand(publication.id(), "Objetivo", slotId));
        currentUser.set("buyer-3");
        var third = service.request(new RequestCommand(publication.id(), "Objetivo", slotId));

        currentUser.set("provider");
        service.accept(new BookingCommand(first.id()));
        service.accept(new BookingCommand(second.id()));
        assertEquals(2, publication.availability().get(0).reserved());
        assertThrows(RuntimeException.class, () -> service.accept(new BookingCommand(third.id())));
        assertEquals(BookingStatus.PENDING, bookings.findById(third.id()).orElseThrow().status());
    }

        @Test
        void onlyProviderCanAcceptOrRejectAndRejectionDoesNotReserveCapacity() {
        Publication publication = Publication.builder("provider")
            .title("Java avanzado")
            .description("Mentoría")
            .category("Tecnología")
            .modality(Modality.REMOTE)
            .durationMinutes(60)
            .capacity(2)
            .basePrice(new SkillCoins(100))
            .availability(List.of(slot()))
            .build();
        publication.publish("provider");
        var publications = new InMemoryPublications(publication);
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        AtomicReference<String> currentUser = new AtomicReference<>("buyer");
        var service = new BookingApplicationService(publications, bookings, wallets,
            new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), event -> {});

        var requested = service.request(new RequestCommand(publication.id(), "Objetivo", publication.availability().get(0).id()));
        assertThrows(RuntimeException.class, () -> service.accept(new BookingCommand(requested.id())));
        assertEquals(BookingStatus.PENDING, bookings.findById(requested.id()).orElseThrow().status());
        assertEquals(0, publication.availability().get(0).reserved());

        currentUser.set("provider");
        var rejected = service.reject(new RejectCommand(requested.id(), "No tengo disponibilidad"));

        assertEquals(BookingStatus.REJECTED, rejected.status());
        assertEquals(0, publication.availability().get(0).reserved());
        }

        @Test
        void requestForUnavailablePublicationDoesNotCreateBooking() {
        Publication publication = Publication.builder("provider")
            .title("Java avanzado")
            .description("Mentoría")
            .category("Tecnología")
            .modality(Modality.REMOTE)
            .durationMinutes(60)
            .capacity(2)
            .basePrice(new SkillCoins(100))
            .availability(List.of(slot()))
            .build();
        publication.publish("provider");
        var bookings = new InMemoryBookings();
        var service = new BookingApplicationService(new InMemoryPublications(publication), bookings,
            new InMemoryWallets(), new StandardRefundPolicy(), () -> "buyer",
            new ImmediateTransactionManager(), event -> {});

        assertThrows(RuntimeException.class,
            () -> service.request(new RequestCommand("missing-publication", "Objetivo", "missing-slot")));
        assertEquals(0, bookings.items.size());
        }

    @Test
    void insufficientBalanceDoesNotConfirmOrMoveFundsIntoEscrow() {
        Publication publication = Publication.builder("provider")
                .title("Java avanzado")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .availability(List.of(slot()))
                .build();
        publication.publish("provider");
        var publications = new InMemoryPublications(publication);
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("buyer", 20));
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        AtomicReference<String> currentUser = new AtomicReference<>("buyer");
        var service = new BookingApplicationService(publications, bookings, wallets,
                new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), event -> {});

        var requested = service.request(new RequestCommand(publication.id(), "Objetivo", publication.availability().get(0).id()));
        currentUser.set("provider");
        service.accept(new BookingCommand(requested.id()));
        currentUser.set("buyer");

        assertThrows(RuntimeException.class, () -> service.pay(new BookingCommand(requested.id())));
        assertEquals(BookingStatus.ACCEPTED, bookings.findById(requested.id()).orElseThrow().status());
        assertEquals(20, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(0, wallets.findByUserId(BookingApplicationService.ESCROW_WALLET_ID).orElseThrow().balance().amount());
    }

    @Test
    void reschedulingTransfersCapacityOnlyAfterBothParticipantsAccept() {
        LocalDateTime firstStart = LocalDateTime.now().plusDays(3);
        AvailabilitySlot firstSlot = AvailabilitySlot.of(firstStart, firstStart.plusHours(1), 2);
        AvailabilitySlot secondSlot = AvailabilitySlot.of(firstStart.plusDays(1), firstStart.plusDays(1).plusHours(1), 2);
        Publication publication = Publication.builder("provider")
                .title("Java avanzado")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .availability(List.of(firstSlot, secondSlot))
                .build();
        publication.publish("provider");

        var publications = new InMemoryPublications(publication);
        var bookings = new InMemoryBookings();
        var wallets = new InMemoryWallets();
        wallets.save(Wallet.open("provider", 0));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        AtomicReference<String> currentUser = new AtomicReference<>("buyer");
        var service = new BookingApplicationService(publications, bookings, wallets,
            new StandardRefundPolicy(), currentUser::get, new ImmediateTransactionManager(), event -> {});

        var requested = service.request(new RequestCommand(publication.id(), "Objetivo", firstSlot.id()));
        currentUser.set("provider");
        service.accept(new BookingCommand(requested.id()));
        service.proposeReschedule(new RescheduleCommand(requested.id(), secondSlot.id()));
        assertEquals(1, firstSlot.reserved());
        assertEquals(0, secondSlot.reserved());

        currentUser.set("buyer");
        var changed = service.acceptReschedule(new BookingCommand(requested.id()));

        assertEquals(secondSlot.id(), changed.slotId());
        assertEquals(0, firstSlot.reserved());
        assertEquals(1, secondSlot.reserved());
    }

    private static AvailabilitySlot slot() {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(3);
        return AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2);
    }

    private static final class ImmediateTransactionManager implements TransactionManager {
        @Override
        public <T> T required(java.util.function.Supplier<T> work) {
            return work.get();
        }
    }

    private static final class InMemoryPublications implements PublicationRepository {
        private final List<Publication> items = new ArrayList<>();

        private InMemoryPublications(Publication publication) {
            items.add(publication);
        }

        @Override
        public Optional<Publication> findById(String publicationId) {
            return items.stream().filter(item -> item.id().equals(publicationId)).findFirst();
        }

        @Override
        public Optional<Publication> findByIdForUpdate(String publicationId) {
            return findById(publicationId);
        }

        @Override
        public List<Publication> findByAuthorUserId(String authorUserId) {
            return items.stream().filter(item -> item.authorUserId().equals(authorUserId)).toList();
        }

        @Override
        public List<Publication> findPublished() {
            return items.stream().filter(item -> item.status() != null).toList();
        }

        @Override
        public ar.edu.uade.skillswap.application.model.PageResult<Publication> searchPublished(
                ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery query) {
            return new ar.edu.uade.skillswap.application.model.PageResult<>(List.of(), query.page(), query.size(), 0);
        }

        @Override
        public Publication save(Publication publication) {
            items.removeIf(item -> item.id().equals(publication.id()));
            items.add(publication);
            return publication;
        }
    }

    private static final class InMemoryBookings implements BookingRepository {
        private final List<Booking> items = new ArrayList<>();

        @Override
        public Optional<Booking> findById(String bookingId) {
            return items.stream().filter(item -> item.id().equals(bookingId)).findFirst();
        }

        @Override
        public Optional<Booking> findByIdForUpdate(String bookingId) {
            return findById(bookingId);
        }

        @Override
        public List<Booking> findByParticipantUserId(String userId) {
            return items.stream().filter(item -> item.buyerUserId().equals(userId) || item.providerUserId().equals(userId)).toList();
        }

        @Override
        public Booking save(Booking booking) {
            items.removeIf(item -> item.id().equals(booking.id()));
            items.add(booking);
            return booking;
        }
    }

    private static final class InMemoryWallets implements WalletRepository {
        private final java.util.Map<String, Wallet> items = new java.util.HashMap<>();

        @Override public Optional<Wallet> findByUserId(String userId) { return Optional.ofNullable(items.get(userId)); }
        @Override public Optional<Wallet> findByUserIdForUpdate(String userId) { return findByUserId(userId); }
        @Override public Wallet save(Wallet wallet) { items.put(wallet.userId(), wallet); return wallet; }
    }
}
