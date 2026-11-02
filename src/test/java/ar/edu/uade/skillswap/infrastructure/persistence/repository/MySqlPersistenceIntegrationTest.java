package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.service.BookingApplicationService;
import ar.edu.uade.skillswap.domain.model.*;
import ar.edu.uade.skillswap.domain.policy.StandardRefundPolicy;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: every test owns a fresh schema, migrated by Flyway, never by Hibernate. */
@EnabledIfSystemProperty(named = "skillswap.mysql.it", matches = "true")
@Timeout(60)
class MySqlPersistenceIntegrationTest {
    private String schema;
    private String baseUrl;
    private String url;
    private String user;
    private String password;
    private EntityManagerFactory factory;
    private JpaTransactionManager transactions;
    private JpaProfileRepository profiles;
    private JpaPublicationRepository publications;
    private JpaBookingRepository bookings;
    private JpaWalletRepository wallets;
    private final AtomicInteger eventCount = new AtomicInteger();

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        baseUrl = requireEnvironment("SKILLSWAP_MYSQL_TEST_URL");
        user = requireEnvironment("SKILLSWAP_MYSQL_TEST_USER");
        password = requireEnvironment("SKILLSWAP_MYSQL_TEST_PASSWORD");
        // A server URL without a database is required. Only our random schema is deleted.
        if (!baseUrl.matches("jdbc:mysql://[^/]+/\\?.+")) {
            throw new IllegalArgumentException("Use jdbc:mysql://host:port/?opciones, sin base de datos");
        }
        schema = "skillswap_it_" + UUID.randomUUID().toString().replace("-", "");
        url = baseUrl.replace("/?", "/" + schema + "?");
        try (var connection = DriverManager.getConnection(baseUrl, user, password);
             var statement = connection.createStatement()) {
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
        }
    }

    @AfterEach
    void removeOnlyOwnedSchema() throws Exception {
        try {
            if (factory != null) factory.close();
        } finally {
            if (schema != null && schema.matches("skillswap_it_[a-f0-9]{32}")) {
                try (var connection = DriverManager.getConnection(baseUrl, user, password);
                     var statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
                }
            }
        }
    }

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").target(target).load();
    }

    private void initializeJpa() {
        flyway("latest").migrate();
        factory = Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", user,
                "jakarta.persistence.jdbc.password", password,
                "hibernate.hbm2ddl.auto", "validate"));
        transactions = new JpaTransactionManager(factory);
        profiles = new JpaProfileRepository(transactions);
        publications = new JpaPublicationRepository(transactions);
        bookings = new JpaBookingRepository(transactions);
        wallets = new JpaWalletRepository(transactions);
    }

    @Test
    void migratesFreshDatabaseAndUpgradesV1WithoutLosingLegacyMessageRows() throws Exception {
        flyway("1").migrate();
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO profiles (id,user_id,display_name,time_zone) VALUES "
                    + "('p1','provider','Ana','UTC'),('p2','buyer','Mateo','UTC')");
            statement.executeUpdate("INSERT INTO publications (id,author_user_id,title,description,category,"
                    + "modality,duration_minutes,capacity,base_price,status,created_at) VALUES "
                    + "('pub','provider','Java','Mentoría','Tecnología','REMOTE',60,1,100,'PUBLISHED',NOW())");
            statement.executeUpdate("INSERT INTO publication_availability "
                    + "(id,publication_id,starts_at,ends_at,capacity) VALUES ('slot','pub',NOW(),DATE_ADD(NOW(),INTERVAL 1 HOUR),1)");
            statement.executeUpdate("INSERT INTO bookings (id,publication_id,buyer_user_id,provider_user_id,"
                    + "objective,slot_id,price,status,created_at) VALUES ('legacy','pub','buyer','provider',"
                    + "'Practicar','slot',100,'PENDING',NOW())");
            statement.executeUpdate("INSERT INTO wallets (user_id,balance) VALUES ('buyer',120)");
            statement.executeUpdate("INSERT INTO wallet_ledger "
                    + "(id,user_id,concept,amount,resulting_balance,occurred_at) VALUES "
                    + "('a','buyer','Inicial',100,100,NOW()),('b','buyer','Crédito',20,120,NOW())");
        }
        initializeJpa(); // upgrades V1 -> latest and validates the real migrated schema
        flyway("latest").validate();
        assertEquals(0, flyway("latest").migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT client_message FROM bookings WHERE id='legacy'")) {
            assertTrue(result.next());
            assertNull(result.getString(1));
        }
        assertEquals("", bookings.findById("legacy").orElseThrow().clientMessage());
        Wallet legacyWallet = wallets.findByUserId("buyer").orElseThrow();
        assertEquals(120, legacyWallet.balance().amount());
        assertEquals(List.of("a", "b"), legacyWallet.movements().stream().map(LedgerEntry::id).toList());
    }

    @Test
    void ledgerPreservesAppendOrderWhenTimestampsTieAndIdsSortInReverse() {
        initializeJpa();
        seed(1, 0);
        LocalDateTime sameTime = LocalDateTime.now().minusHours(1).withNano(0);
        Wallet wallet = new Wallet("buyer", new SkillCoins(110), List.of(
                new LedgerEntry("z-first", "Inicial", 100, 100, sameTime),
                new LedgerEntry("a-second", "Crédito", 10, 110, sameTime)));
        transactions.required(() -> wallets.save(wallet));
        Wallet loaded = wallets.findByUserId("buyer").orElseThrow();
        assertEquals(110, loaded.balance().amount());
        assertEquals(List.of("z-first", "a-second"), loaded.movements().stream().map(LedgerEntry::id).toList());
    }

    @Test
    void preservesMessageThroughSaveLoadAndStateUpdateOnMySql() {
        initializeJpa();
        Publication publication = seed(2, 500);
        var requested = service("buyer").request(command(publication));
        service("provider").accept(new BookingCommand(requested.id()));
        Booking loaded = bookings.findById(requested.id()).orElseThrow();
        assertEquals("¿Repasamos diseño?", loaded.clientMessage());
        assertEquals("Practicar Java", loaded.objective());
        assertEquals(BookingStatus.ACCEPTED, loaded.status());
    }

    @Test
    void rollsBackFlushedPaymentBalancesLedgerStatusAndAfterCommitEvents() {
        initializeJpa();
        Publication publication = seed(1, 500);
        var requested = service("buyer").request(command(publication));
        var bookingCommand = new BookingCommand(requested.id());
        service("provider").accept(bookingCommand);
        int eventsBefore = eventCount.get();
        assertThrows(InjectedFailure.class, () -> transactions.required(() -> {
            service("buyer").pay(bookingCommand);
            transactions.currentEntityManager().flush(); // SQL really reaches MySQL before rollback
            throw new InjectedFailure();
        }));
        assertEquals(BookingStatus.ACCEPTED, bookings.findById(requested.id()).orElseThrow().status());
        assertEquals(500, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(1, wallets.findByUserId("buyer").orElseThrow().movements().size());
        assertEquals(0, wallets.findByUserId("provider").orElseThrow().balance().amount());
        assertTrue(wallets.findByUserId("provider").orElseThrow().movements().isEmpty());
        assertEquals(eventsBefore, eventCount.get());
        service("buyer").pay(bookingCommand); // context and connection remain usable after rollback
        assertEquals(400, wallets.findByUserId("buyer").orElseThrow().balance().amount());
    }

    @RepeatedTest(3)
    void competingAcceptancesCannotOverbookLastSeat() throws Exception {
        initializeJpa();
        Publication publication = seed(1, 500);
        var first = service("buyer").request(command(publication));
        var second = service("buyer2").request(command(publication));
        int before = eventCount.get();
        assertSingleWinner(() -> service("provider").accept(new BookingCommand(first.id())),
                () -> service("provider").accept(new BookingCommand(second.id())));
        assertEquals(1, publications.findById(publication.id()).orElseThrow().availability().get(0).reserved());
        long accepted = List.of(first.id(), second.id()).stream()
                .map(id -> bookings.findById(id).orElseThrow())
                .filter(b -> b.status() == BookingStatus.ACCEPTED).count();
        assertEquals(1, accepted);
        assertEquals(before + 1, eventCount.get());
    }

    @Test
    void failedAcceptanceReleasesFlushedSeatAndDoesNotPublishEvent() {
        initializeJpa();
        Publication publication = seed(1, 500);
        var requested = service("buyer").request(command(publication));
        int before = eventCount.get();
        assertThrows(InjectedFailure.class, () -> transactions.required(() -> {
            service("provider").accept(new BookingCommand(requested.id()));
            transactions.currentEntityManager().flush();
            throw new InjectedFailure();
        }));
        assertEquals(BookingStatus.PENDING, bookings.findById(requested.id()).orElseThrow().status());
        assertEquals(0, publications.findById(publication.id()).orElseThrow().availability().get(0).reserved());
        assertEquals(before, eventCount.get());
        service("provider").accept(new BookingCommand(requested.id()));
        assertEquals(1, publications.findById(publication.id()).orElseThrow().availability().get(0).reserved());
    }

    @RepeatedTest(3)
    void simultaneousPaymentsOfSameBookingDebitAndCreditOnlyOnce() throws Exception {
        initializeJpa();
        Publication publication = seed(1, 500);
        var requested = service("buyer").request(command(publication));
        var command = new BookingCommand(requested.id());
        service("provider").accept(command);
        int before = eventCount.get();
        assertSingleWinner(() -> service("buyer").pay(command), () -> service("buyer").pay(command));
        assertEquals(BookingStatus.CONFIRMED, bookings.findById(requested.id()).orElseThrow().status());
        assertEquals(400, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(100, wallets.findByUserId("provider").orElseThrow().balance().amount());
        assertEquals(2, wallets.findByUserId("buyer").orElseThrow().movements().size());
        assertEquals(1, wallets.findByUserId("provider").orElseThrow().movements().size());
        assertEquals(before + 1, eventCount.get());
    }

    @Test
    void concurrentDifferentBookingsCannotSpendTheSameBalanceTwice() throws Exception {
        initializeJpa();
        Publication publication = seed(2, 100);
        var first = service("buyer").request(command(publication));
        var second = service("buyer").request(command(publication));
        service("provider").accept(new BookingCommand(first.id()));
        service("provider").accept(new BookingCommand(second.id()));
        assertSingleWinner(() -> service("buyer").pay(new BookingCommand(first.id())),
                () -> service("buyer").pay(new BookingCommand(second.id())));
        assertEquals(0, wallets.findByUserId("buyer").orElseThrow().balance().amount());
        assertEquals(100, wallets.findByUserId("provider").orElseThrow().balance().amount());
        assertEquals(1, List.of(first.id(), second.id()).stream()
                .filter(id -> bookings.findById(id).orElseThrow().status() == BookingStatus.CONFIRMED).count());
    }

    @Test
    void filtersCountsAndPaginatesWithoutDuplicatesIncludingEmptyPage() {
        initializeJpa();
        seed(1, 500);
        List<String> expected = new ArrayList<>();
        transactions.required(() -> {
            for (int price = 10; price <= 50; price += 10) {
                Publication publication = newPublication(1, price, "Paginación");
                publications.save(publication);
                expected.add(publication.id());
            }
            Publication draft = newPublication(1, 5, "Paginación");
            draft.pause("provider");
            publications.save(draft); // excluded by status
            return null;
        });
        List<String> actual = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            var result = publications.searchPublished(search(page, 10, 50));
            assertEquals(5, result.totalElements());
            assertEquals(3, result.totalPages());
            assertEquals(page == 2 ? 1 : 2, result.items().size());
            actual.addAll(result.items().stream().map(Publication::id).toList());
        }
        assertEquals(expected, actual);
        assertEquals(5, new HashSet<>(actual).size());
        assertTrue(publications.searchPublished(search(3, 10, 50)).items().isEmpty());
        var filtered = publications.searchPublished(search(0, 20, 30));
        assertEquals(2, filtered.totalElements());
        assertEquals(List.of(20, 30), filtered.items().stream().map(p -> p.basePrice().amount()).toList());
    }

    private SearchQuery search(int page, int minimum, int maximum) {
        return new SearchQuery("Java", "Paginación", Modality.REMOTE, minimum, maximum,
                0.0, SearchOrder.LOWEST_PRICE, page, 2);
    }

    @Test
    void paginationHasStableBoundariesWhenPriceAndCreationTimeTie() {
        initializeJpa();
        seed(1, 500);
        List<String> expected = transactions.required(() -> {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Publication publication = newPublication(1, 20, "Paginación");
                publications.save(publication);
                ids.add(publication.id());
            }
            transactions.currentEntityManager().flush();
            transactions.currentEntityManager().createNativeQuery(
                    "UPDATE publications SET created_at = '2026-01-01 10:00:00' WHERE category = :category")
                    .setParameter("category", "Paginación").executeUpdate();
            return ids.stream().sorted().toList();
        });
        List<String> actual = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            actual.addAll(publications.searchPublished(search(page, 20, 20)).items()
                    .stream().map(Publication::id).toList());
        }
        assertEquals(expected, actual);
        assertEquals(5, new HashSet<>(actual).size());
    }

    private Publication seed(int capacity, int balance) {
        return transactions.required(() -> {
            for (String id : List.of("provider", "buyer", "buyer2")) {
                profiles.save(Profile.register(id, id, "", "UTC", List.of(), List.of(),
                        Set.of(NotificationChannelType.IN_APP), List.of()));
                wallets.save(Wallet.open(id, id.equals("buyer") ? balance : 0));
            }
            Publication publication = newPublication(capacity, 100, "Tecnología");
            publications.save(publication);
            return publication;
        });
    }

    private Publication newPublication(int capacity, int price, String category) {
        LocalDateTime start = LocalDateTime.now().plusDays(7).withNano(0);
        Publication publication = Publication.builder("provider").title("Java")
                .description("Mentoría Java").category(category).modality(Modality.REMOTE)
                .durationMinutes(60).capacity(capacity).basePrice(new SkillCoins(price))
                .addAvailability(AvailabilitySlot.of(start, start.plusHours(1), capacity)).build();
        publication.publish("provider");
        return publication;
    }

    private RequestCommand command(Publication publication) {
        return new RequestCommand(publication.id(), "¿Repasamos diseño?", "Practicar Java",
                publication.availability().get(0).id());
    }

    private BookingApplicationService service(String actor) {
        return new BookingApplicationService(bookings, publications, wallets, () -> actor, transactions,
                new StandardRefundPolicy(), event -> eventCount.incrementAndGet());
    }

    private void assertSingleWinner(Runnable first, Runnable second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Queue<String> rejections = new ConcurrentLinkedQueue<>();
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (Runnable action : List.of(first, second)) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("No se liberó la carrera");
                    try {
                        action.run();
                        return true;
                    } catch (ApplicationException expectedBusinessRejection) {
                        rejections.add(expectedBusinessRejection.getMessage());
                        // SQL errors, deadlocks and timeouts must fail the test, not count as valid rejection.
                        assertNotNull(expectedBusinessRejection.getCause());
                        assertTrue(expectedBusinessRejection.getCause() instanceof IllegalStateException
                                || expectedBusinessRejection.getCause() instanceof IllegalArgumentException);
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int winners = 0;
            for (Future<Boolean> result : results) if (result.get(20, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners, () -> "Rechazos: " + rejections);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta " + name);
        return value;
    }

    private static final class InjectedFailure extends RuntimeException {}
}
