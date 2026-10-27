package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.RatingApplicationMapper;
import ar.edu.uade.skillswap.application.model.RatingData.RatingView;
import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;
import ar.edu.uade.skillswap.application.port.in.RatingUseCases;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Rating;
import java.util.ArrayList;
import java.util.Objects;

/** Coordinates rating submission, duplicate checks, and derived profile reputation. */
public final class RatingApplicationService implements RatingUseCases {
    private final BookingRepository bookings;
    private final RatingRepository ratings;
    private final ProfileRepository profiles;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;

    public RatingApplicationService(BookingRepository bookings, RatingRepository ratings,
                                    ProfileRepository profiles, CurrentUserProvider currentUser,
                                    TransactionManager transactions) {
        this.bookings = Objects.requireNonNull(bookings);
        this.ratings = Objects.requireNonNull(ratings);
        this.profiles = Objects.requireNonNull(profiles);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public RatingView submit(SubmitCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la calificación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String bookingId = UseCaseSupport.requiredId(command.bookingId(), "contratación");

        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            var booking = bookings.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> notFound("No existe la contratación indicada"));
            if (ratings.existsByBookingIdAndAuthorUserId(bookingId, actor)) {
                throw new ApplicationException(ApplicationErrorCode.CONFLICT,
                        "Ya calificaste esta contratación");
            }

            Rating rating = Rating.forCompletedBooking(booking, actor, command.score(), command.comment());
            Profile subject = profiles.findByUserIdForUpdate(rating.subjectUserId())
                    .orElseThrow(() -> notFound("No existe el perfil de quien recibe la calificación"));
            var verifiedRatings = new ArrayList<>(ratings.findBySubjectUserId(rating.subjectUserId()));
            verifiedRatings.add(rating);
            subject.recalculateReputation(verifiedRatings);

            Rating savedRating = ratings.save(rating);
            profiles.save(subject);
            return RatingApplicationMapper.toView(savedRating);
        }));
    }

    private static ApplicationException notFound(String message) {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, message);
    }
}
