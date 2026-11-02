
package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.RatingEntity;

public final class RatingMapper {

    private RatingMapper() {
    }

    public static RatingEntity toEntity(Rating rating, BookingEntity booking) {
        if (rating == null) {
            throw new IllegalArgumentException("La calificación no puede ser null");
        }

        if (booking == null) {
            throw new IllegalArgumentException("La reserva no puede ser null");
        }

        if (!rating.bookingId().equals(booking.getId())) {
            throw new IllegalArgumentException(
                    "La calificación y la reserva no coinciden"
            );
        }

        RatingEntity entity = new RatingEntity();
        entity.setId(rating.id());
        entity.setBooking(booking);
        entity.setAuthorUserId(rating.authorUserId());
        entity.setSubjectUserId(rating.subjectUserId());
        entity.setScore(rating.score());
        entity.setComment(rating.comment());
        entity.setCreatedAt(rating.createdAt());

        return entity;
    }

    public static Rating toDomain(RatingEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "La entidad de calificación no puede ser null"
            );
        }

        return Rating.restore(
                entity.getId(),
                entity.getBooking().getId(),
                entity.getAuthorUserId(),
                entity.getSubjectUserId(),
                entity.getScore(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }
}
