package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.RatingData.RatingView;
import ar.edu.uade.skillswap.domain.model.Rating;

/** Converts a verified domain rating into the application output model. */
public final class RatingApplicationMapper {
    private RatingApplicationMapper() {}

    public static RatingView toView(Rating rating) {
        return new RatingView(rating.id(), rating.bookingId(), rating.authorUserId(),
                rating.subjectUserId(), rating.score(), rating.comment(), rating.createdAt());
    }
}
