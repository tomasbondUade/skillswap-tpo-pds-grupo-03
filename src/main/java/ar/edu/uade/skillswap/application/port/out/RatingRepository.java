package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Rating;
import java.util.List;

/** Persistence contract for verified ratings. */
public interface RatingRepository {
    boolean existsByBookingIdAndAuthorUserId(String bookingId, String authorUserId);
    List<Rating> findBySubjectUserId(String subjectUserId);
    Rating save(Rating rating);
}
