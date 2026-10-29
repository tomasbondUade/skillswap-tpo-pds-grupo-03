package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.mapper.RatingApplicationMapper;
import ar.edu.uade.skillswap.application.model.RatingData.RatingView;
import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;
import ar.edu.uade.skillswap.application.port.in.RatingUseCases;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.DomainEventPublisher;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Rating;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static ar.edu.uade.skillswap.application.error.ApplicationErrorCode.NOT_FOUND;

/**
 * Coordina el envío de calificaciones, las validaciones de duplicados
 * y la actualización de la reputación del perfil.
 */
public final class RatingApplicationService implements RatingUseCases {

    private final BookingRepository bookings;
    private final RatingRepository ratings;
    private final ProfileRepository profiles;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;
    private final DomainEventPublisher events;

    /**
     * Constructor que permite inyectar el publicador de eventos.
     */
    public RatingApplicationService(
            BookingRepository bookings,
            RatingRepository ratings,
            ProfileRepository profiles,
            CurrentUserProvider currentUser,
            TransactionManager transactions,
            DomainEventPublisher events) {

        this.bookings = Objects.requireNonNull(bookings);
        this.ratings = Objects.requireNonNull(ratings);
        this.profiles = Objects.requireNonNull(profiles);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
        this.events = Objects.requireNonNull(events);
    }

    @Override
    public RatingView submit(SubmitCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la calificación");

        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(
                command.bookingId(),
                "contratación"
        );

        return transactions.required(() ->
                UseCaseSupport.translateDomainFailures(() -> {

                    var booking = bookings.findByIdForUpdate(bookingId)
                            .orElseThrow(() ->
                                    notFound("No existe la contratación indicada"));

                    if (ratings.existsByBookingIdAndAuthorUserId(
                            bookingId, actor)) {
                        throw new ApplicationException(
                                ApplicationErrorCode.CONFLICT,
                                "Ya calificaste esta contratación"
                        );
                    }

                    Rating rating = Rating.forCompletedBooking(
                            booking,
                            actor,
                            command.score(),
                            command.comment()
                    );

                    Profile subject = profiles.findByUserIdForUpdate(
                                    rating.subjectUserId())
                            .orElseThrow(() ->
                                    notFound("No existe el perfil de quien recibe la calificación"));

                    var verifiedRatings = new ArrayList<>(
                            ratings.findBySubjectUserId(rating.subjectUserId())
                    );

                    verifiedRatings.add(rating);
                    subject.recalculateReputation(verifiedRatings);

                    // Guardamos la calificación y actualizamos la reputación.
                    Rating savedRating = ratings.save(rating);
                    profiles.save(subject);

                    // Construimos un evento independiente de los proveedores
                    // concretos de notificaciones.
                    DomainEvent event = new DomainEvent(
                            "rating.submitted",
                            "Rating",
                            savedRating.id(),
                            savedRating.authorUserId(),
                            savedRating.createdAt(),
                            Map.of(
                                    "subjectUserId", savedRating.subjectUserId(),
                                    "bookingId", savedRating.bookingId(),
                                    "score", String.valueOf(savedRating.score()),
                                    "comment", savedRating.comment()
                            )
                    );

                    // El evento se publica después de confirmar la transacción.
                    transactions.afterCommit(() -> events.publish(event));

                    return RatingApplicationMapper.toView(savedRating);
                })
        );
    }

    private static ApplicationException notFound(String message) {
        return new ApplicationException(NOT_FOUND, message);
    }
}
