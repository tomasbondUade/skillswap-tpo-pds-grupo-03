package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
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
import java.util.Set;
import java.util.UUID;

/** Minimal application wiring for the current mock/in-memory setup. */
public final class SkillSwapApplicationWiring {
    private final BookingRepository bookings = new InMemoryBookingRepository();
    private final WalletRepository wallets = new InMemoryWalletRepository();
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final InMemoryDomainEventPublisher events = new InMemoryDomainEventPublisher();
    private final NotificationEventObserver notificationObserver =
            new NotificationEventObserver(new InAppNotificationGateway(notifications));
    private final RatingRepository ratings = new InMemoryRatingRepository();
    private final ProfileRepository profiles = new InMemoryProfileRepository();
    private final PublicationRepository publications = new InMemoryPublicationRepository();
    private final CurrentUserProvider currentUser = new DemoCurrentUserProvider();
    private final TransactionManager transactions = new SynchronousTransactionManager();
    private final RecommendationStrategy recommendationStrategy = new SkillAffinityStrategy();
    private final RefundPolicy refundPolicy = new StandardRefundPolicy();

    private final ProfileApplicationService profileService = new ProfileApplicationService(profiles, currentUser, transactions);
    private final PublicationApplicationService publicationService = new PublicationApplicationService(publications, currentUser, transactions);
    private final DiscoveryApplicationService discoveryService = new DiscoveryApplicationService(publications, profiles, currentUser, recommendationStrategy);
    private final BookingApplicationService bookingService = new BookingApplicationService(
            publications, bookings, wallets, refundPolicy, currentUser, transactions, events);
    private final RatingApplicationService ratingService = new RatingApplicationService(
            bookings, ratings, profiles, currentUser, transactions, events);
    private final WalletApplicationService walletService = new WalletApplicationService(wallets, currentUser, transactions);
    private final NotificationApplicationService notificationService = new NotificationApplicationService(notifications, currentUser, transactions);

    public SkillSwapApplicationWiring() {
        subscribeNotifications();
        seedDemoData();
    }

    private void subscribeNotifications() {
        List.of("booking.requested", "booking.accepted", "booking.rejected", "booking.paid",
                "booking.reschedule-proposed", "booking.reschedule-accepted", "booking.rescheduled",
                "booking.cancelled", "booking.started", "booking.completed", "rating.submitted")
                .forEach(eventName -> events.subscribe(eventName, notificationObserver::onEvent));
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
        seedProfile("mentor-ana", "Ana García", List.of("Entrevistas", "Backend"));
        seedProfile("mentor-lu", "Lucía Pérez", List.of("UX", "Diseño"));
        wallets.save(Wallet.open(currentUser.currentUserId(), 2480));
        wallets.save(Wallet.open(BookingApplicationService.ESCROW_WALLET_ID, 0));
        notifications.save(new NotificationView(
                "welcome-notification",
                "Bienvenido a SkillSwap",
                "Tu espacio para aprender y compartir habilidades ya está listo.",
                LocalDateTime.now().minusMinutes(20),
                false,
                "BIENVENIDA"), currentUser.currentUserId());

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

    private void seedProfile(String userId, String displayName, List<String> skills) {
        if (profiles.findByUserId(userId).isEmpty()) {
            profiles.save(Profile.register(userId, displayName,
                    "Mentor de la comunidad SkillSwap.", "America/Argentina/Buenos_Aires",
                    skills, List.of(), Set.of(NotificationChannelType.IN_APP), List.of()));
        }
    }

    private void seedPublishedPublication(String authorId, String title, String description, String category,
                                          Modality modality, int durationMinutes, int price,
                                          Set<String> tags) {
        wallets.findByUserId(authorId).orElseGet(() -> wallets.save(Wallet.open(authorId, 0)));
        boolean exists = publications.findPublished().stream()
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

    public CurrentUserProvider currentUserProvider() {
        return currentUser;
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
