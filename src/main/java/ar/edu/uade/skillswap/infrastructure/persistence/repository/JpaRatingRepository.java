
package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.RatingEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.RatingMapper;

import java.util.List;
import java.util.Objects;

public final class JpaRatingRepository extends JpaRepositorySupport
        implements RatingRepository {

    public JpaRatingRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public boolean existsByBookingIdAndAuthorUserId(
            String bookingId,
            String authorUserId) {

        Objects.requireNonNull(
                bookingId, "Falta el identificador de la reserva");
        Objects.requireNonNull(
                authorUserId, "Falta el identificador del autor");

        return query(em -> em.createQuery(
                        "SELECT COUNT(r) FROM RatingEntity r "
                                + "WHERE r.booking.id = :bookingId "
                                + "AND r.authorUserId = :authorUserId",
                        Long.class)
                .setParameter("bookingId", bookingId)
                .setParameter("authorUserId", authorUserId)
                .getSingleResult() > 0);
    }

    @Override
    public List<Rating> findBySubjectUserId(String subjectUserId) {
        Objects.requireNonNull(
                subjectUserId, "Falta el identificador del destinatario");

        return query(em -> em.createQuery(
                        "SELECT r FROM RatingEntity r "
                                + "WHERE r.subjectUserId = :subjectUserId "
                                + "ORDER BY r.createdAt DESC, r.id ASC",
                        RatingEntity.class)
                .setParameter("subjectUserId", subjectUserId)
                .getResultList()
                .stream()
                .map(RatingMapper::toDomain)
                .toList());
    }

    @Override
    public Rating save(Rating rating) {
        Objects.requireNonNull(rating, "Falta la calificación");

        BookingEntity booking = entityManager().find(
                BookingEntity.class, rating.bookingId());

        if (booking == null) {
            throw new IllegalArgumentException(
                    "No existe la reserva de la calificación");
        }

        if (entityManager().find(RatingEntity.class, rating.id()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe una calificación con ese identificador");
        }

        if (existsByBookingIdAndAuthorUserId(
                rating.bookingId(), rating.authorUserId())) {
            throw new IllegalArgumentException(
                    "El autor ya calificó esta reserva");
        }

        RatingEntity entity = RatingMapper.toEntity(rating, booking);
        entityManager().persist(entity);

        return RatingMapper.toDomain(entity);
    }
}
