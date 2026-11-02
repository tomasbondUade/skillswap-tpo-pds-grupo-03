package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.infrastructure.config.FlywayConfig;
import ar.edu.uade.skillswap.infrastructure.config.HibernateConfig;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaBookingRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaNotificationRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaProfileRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaPublicationRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaRatingRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.repository.JpaWalletRepository;
import jakarta.persistence.EntityManagerFactory;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.application.service.BookingApplicationService;
import ar.edu.uade.skillswap.application.service.DiscoveryApplicationService;
import ar.edu.uade.skillswap.application.service.NotificationApplicationService;
import ar.edu.uade.skillswap.application.service.ProfileApplicationService;
import ar.edu.uade.skillswap.application.service.PublicationApplicationService;
import ar.edu.uade.skillswap.application.service.RatingApplicationService;
import ar.edu.uade.skillswap.application.service.WalletApplicationService;
import ar.edu.uade.skillswap.infrastructure.identity.IdentityProviderAdapter;
import ar.edu.uade.skillswap.infrastructure.identity.MockIdentityClient;
import ar.edu.uade.skillswap.infrastructure.notification.NotificationFactory;
import ar.edu.uade.skillswap.infrastructure.notification.RatingNotificationEventPublisher;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.RecommendationStrategy;
import ar.edu.uade.skillswap.domain.model.SkillAffinityStrategy;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.domain.policy.RefundPolicy;
import ar.edu.uade.skillswap.domain.policy.StandardRefundPolicy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Composition root shared by the in-memory demo and the JPA execution mode. */
public final class SkillSwapApplicationWiring implements AutoCloseable {
    private final BookingRepository bookings;
    private final WalletRepository wallets;
    private final NotificationRepository notifications;
    private final InMemoryDomainEventPublisher events = new InMemoryDomainEventPublisher();
    private final NotificationEventObserver notificationObserver;
    private final RatingNotificationEventPublisher ratingNotifications;
    private final RatingRepository ratings;
    private final ProfileRepository profiles;
    private final PublicationRepository publications;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;
    private final Runnable closeResources;
    private final RecommendationStrategy recommendationStrategy = new SkillAffinityStrategy();
    private final RefundPolicy refundPolicy = new StandardRefundPolicy();

    private final ProfileApplicationService profileService;
    private final PublicationApplicationService publicationService;
    private final DiscoveryApplicationService discoveryService;
    private final BookingApplicationService bookingService;
    private final RatingApplicationService ratingService;
    private final WalletApplicationService walletService;
    private final NotificationApplicationService notificationService;

    public SkillSwapApplicationWiring() {
        this(new MockIdentityClient("user-mateo"), new MockEmailClient(), new MockPushClient());
    }

    public SkillSwapApplicationWiring(MockIdentityClient identityClient,
                                      MockEmailClient emailClient, MockPushClient pushClient) {
        this(identityClient, emailClient, pushClient, new InMemoryProfileRepository());
    }

    public SkillSwapApplicationWiring(MockIdentityClient identityClient,
                                      MockEmailClient emailClient, MockPushClient pushClient,
                                      ProfileRepository profiles) {
        this(identityClient, emailClient, pushClient, profiles,
                new InMemoryPublicationRepository(), new InMemoryBookingRepository(),
                new InMemoryWalletRepository(), new InMemoryRatingRepository(),
                new InMemoryNotificationRepository(), new SynchronousTransactionManager(), () -> {});
    }

    /** Runs Flyway before Hibernate validates the database configured through the environment. */
    public static SkillSwapApplicationWiring persistent() {
        FlywayConfig.migrate();
        return usingJpa(HibernateConfig.createEntityManagerFactory(),
                new MockIdentityClient("user-mateo"), new MockEmailClient(), new MockPushClient());
    }

    /** Takes ownership of the factory; close the wiring to release its connections. */
    public static SkillSwapApplicationWiring usingJpa(EntityManagerFactory factory,
            MockIdentityClient identity, MockEmailClient email, MockPushClient push) {
        Objects.requireNonNull(factory, "Falta la fábrica JPA");
        try {
            var tx = new JpaTransactionManager(factory);
            return new SkillSwapApplicationWiring(identity, email, push,
                    new JpaProfileRepository(tx), new JpaPublicationRepository(tx),
                    new JpaBookingRepository(tx), new JpaWalletRepository(tx),
                    new JpaRatingRepository(tx), new JpaNotificationRepository(tx), tx,
                    () -> { if (factory.isOpen()) factory.close(); });
        } catch (RuntimeException | Error error) {
            factory.close();
            throw error;
        }
    }

    private SkillSwapApplicationWiring(MockIdentityClient identityClient,
            MockEmailClient emailClient, MockPushClient pushClient, ProfileRepository profiles,
            PublicationRepository publications, BookingRepository bookings, WalletRepository wallets,
            RatingRepository ratings, NotificationRepository notifications,
            TransactionManager transactions, Runnable closeResources) {
        this.profiles = Objects.requireNonNull(profiles);
        this.publications = Objects.requireNonNull(publications);
        this.bookings = Objects.requireNonNull(bookings);
        this.wallets = Objects.requireNonNull(wallets);
        this.ratings = Objects.requireNonNull(ratings);
        this.notifications = Objects.requireNonNull(notifications);
        this.transactions = Objects.requireNonNull(transactions);
        this.closeResources = Objects.requireNonNull(closeResources);
        currentUser = new IdentityProviderAdapter(identityClient);
        NotificationFactory factory = new NotificationFactory(notifications, emailClient, pushClient);
        notificationObserver = new NotificationEventObserver(profiles, factory);
        ratingNotifications = new RatingNotificationEventPublisher(profiles, factory);

        profileService = new ProfileApplicationService(profiles, currentUser, transactions);
        publicationService = new PublicationApplicationService(publications, currentUser, transactions);
        discoveryService = new DiscoveryApplicationService(publications, profiles, currentUser, recommendationStrategy);
        bookingService = new BookingApplicationService(bookings, publications, wallets,
                currentUser, transactions, refundPolicy, events);
        ratingService = new RatingApplicationService(bookings, ratings, profiles, currentUser, transactions, events);
        walletService = new WalletApplicationService(wallets, currentUser, transactions);
        notificationService = new NotificationApplicationService(notifications, currentUser, transactions);

        subscribeNotifications();
        transactions.required(() -> { seedDemoData(); return null; });
    }

    @Override
    public void close() {
        closeResources.run();
    }

    private void subscribeNotifications() {
        List.of("booking.requested", "booking.accepted", "booking.rejected", "booking.paid",
                "booking.reschedule-proposed", "booking.reschedule-accepted", "booking.rescheduled",
                "booking.cancelled", "booking.started", "booking.completed")
                .forEach(eventName -> events.subscribe(eventName, notificationObserver::onEvent));
        events.subscribe("rating.submitted", ratingNotifications::publish);
    }

    private void seedDemoData() {
        if (profiles.findByUserId(currentUser.currentUserId()).isEmpty()) {
            profiles.save(Profile.register(
                currentUser.currentUserId(),
                "Mateo Silva",
                "Construyo productos digitales y acompaño personas que quieren crecer en tecnología.",
                "America/Argentina/Buenos_Aires",
                List.of("Java", "Clean Architecture", "Mentoría", "SQL"),
                List.of("Tecnología", "Carrera", "Productividad"),
                Set.of(NotificationChannelType.IN_APP),
                List.of()
            ));
        }
        seedProfile("mentor-ana", "Ana García", List.of("Entrevistas", "Backend"),
                Set.of(NotificationChannelType.EMAIL, NotificationChannelType.PUSH, NotificationChannelType.IN_APP));
        seedProfile("mentor-lu", "Lucía Pérez", List.of("UX", "Diseño"),
                Set.of(NotificationChannelType.IN_APP));
        wallets.findByUserId(currentUser.currentUserId()).orElseGet(() ->
                wallets.save(Wallet.open(currentUser.currentUserId(), 2480)));
        if (notifications.findByRecipientUserId(currentUser.currentUserId()).stream()
                .noneMatch(item -> "BIENVENIDA".equals(item.type()))) {
            notifications.save(new NotificationMessage(currentUser.currentUserId(),
                "Bienvenido a SkillSwap",
                "Tu espacio para aprender y compartir habilidades ya está listo.",
                "BIENVENIDA", NotificationChannelType.IN_APP));
        }

        seedPublishedPublication(
            "user-mateo",
            "Java para arquitectura limpia",
            "Sesión práctica para ordenar dependencias, casos de uso y decisiones de diseño en Java.",
            "Tecnología",
            Modality.REMOTE,
            90,
            220,
            Set.of("Java", "Arquitectura", "Clean Code")
        );

        seedPublishedPublication(
            "mentor-ana",
            "Entrevistas técnicas con feedback real",
            "Simulacro de entrevista, feedback concreto y plan de mejora para tu próximo proceso.",
            "Tecnología",
            Modality.HYBRID,
            60,
            180,
            Set.of("Entrevistas", "Backend", "Carrera")
        );

        seedPublishedPublication(
            "mentor-lu",
            "Diseño de portfolio para perfiles digitales",
            "Te ayudo a ordenar tu portfolio, contar mejor tus proyectos y destacar tu valor.",
            "Diseño",
            Modality.IN_PERSON,
            45,
            150,
            Set.of("Portfolio", "UX", "Diseño")
        );
    }

    private void seedProfile(String userId, String displayName, List<String> skills,
                             Set<NotificationChannelType> channels) {
        if (profiles.findByUserId(userId).isEmpty()) {
            profiles.save(Profile.register(userId, displayName,
                    "Mentor de la comunidad SkillSwap.", "America/Argentina/Buenos_Aires",
                    skills, List.of(), channels, List.of()));
        }
    }

    private void seedPublishedPublication(String authorId, String title, String description, String category,
                                          Modality modality, int durationMinutes, int price,
                                          Set<String> tags) {
        wallets.findByUserId(authorId).orElseGet(() -> wallets.save(Wallet.open(authorId, 0)));
        boolean exists = publications.findByAuthorUserId(authorId).stream()
            .anyMatch(item -> item.title().equalsIgnoreCase(title));
        if (exists) {
            return;
        }

        var publication = Publication.builder(authorId)
            .title(title)
            .description(description)
            .category(category)
            .modality(modality)
            .durationMinutes(durationMinutes)
            .capacity(6)
            .basePrice(new SkillCoins(price))
            .tags(tags)
            .addAvailability(new AvailabilitySlot("slot-" + UUID.randomUUID(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                6,
                0))
            .build();
        publication.publish(authorId);
        publications.save(publication);
    }

    public ProfileApplicationService profileService() {
        return profileService;
    }

    public PublicationApplicationService publicationService() {
        return publicationService;
    }

    public DiscoveryApplicationService discoveryService() {
        return discoveryService;
    }

    public BookingApplicationService bookingService() {
        return bookingService;
    }

    public WalletApplicationService walletService() {
        return walletService;
    }

    public NotificationApplicationService notificationService() {
        return notificationService;
    }

    public RatingApplicationService ratingService() {
        return ratingService;
    }
}
