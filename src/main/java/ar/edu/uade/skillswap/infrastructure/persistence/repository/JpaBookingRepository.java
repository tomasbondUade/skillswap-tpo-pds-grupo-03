
package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.StatusHistoryEntry;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingStatusHistoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.BookingMapper;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación JPA del repositorio de reservas.
 */
public final class JpaBookingRepository extends JpaRepositorySupport
        implements BookingRepository {

    public JpaBookingRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        Objects.requireNonNull(
                bookingId, "Falta el identificador de la reserva");

        return query(em -> Optional.ofNullable(
                        em.find(BookingEntity.class, bookingId))
                .map(BookingMapper::toDomain));
    }

    @Override
    public Optional<Booking> findByIdForUpdate(String bookingId) {
        Objects.requireNonNull(
                bookingId, "Falta el identificador de la reserva");

        return query(em -> em.createQuery(
                        "SELECT b FROM BookingEntity b "
                                + "WHERE b.id = :id",
                        BookingEntity.class)
                .setParameter("id", bookingId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .map(BookingMapper::toDomain));
    }

    @Override
    public java.util.List<Booking> findByParticipantUserId(String userId) {
        Objects.requireNonNull(
                userId, "Falta el identificador del participante");

        return query(em -> em.createQuery(
                        "SELECT b FROM BookingEntity b "
                                + "WHERE b.buyerUserId = :userId "
                                + "OR b.providerUserId = :userId "
                                + "ORDER BY b.createdAt DESC",
                        BookingEntity.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(BookingMapper::toDomain)
                .toList());
    }

    @Override
    public Booking save(Booking booking) {
        Objects.requireNonNull(booking, "Falta la reserva");

        BookingEntity existing =
                entityManager().find(BookingEntity.class, booking.id());

        if (existing == null) {
            PublicationEntity publication = entityManager().find(
                    PublicationEntity.class, booking.publicationId());

            if (publication == null) {
                throw new IllegalArgumentException(
                        "No existe la publicación de la reserva");
            }

            BookingEntity entity =
                    BookingMapper.toEntity(booking, publication);

            entityManager().persist(entity);

            return BookingMapper.toDomain(entity);
        }

        if (!existing.getPublication().getId()
                .equals(booking.publicationId())) {
            throw new IllegalArgumentException(
                    "No se puede cambiar la publicación de una reserva");
        }

        existing.setBuyerUserId(booking.buyerUserId());
        existing.setProviderUserId(booking.providerUserId());
        existing.setObjective(booking.objective());
        existing.setSlotId(booking.slotId());
        existing.setProposedSlotId(booking.proposedSlotId());
        existing.setBuyerAcceptedReschedule(
                booking.buyerAcceptedReschedule());
        existing.setProviderAcceptedReschedule(
                booking.providerAcceptedReschedule());
        existing.setPrice(booking.price().amount());
        existing.setRefund(booking.refund().amount());
        existing.setStatus(booking.status().name());
        existing.setPaidAt(booking.paidAt());

        synchronizeHistory(existing, booking.history());

        return BookingMapper.toDomain(existing);
    }

    /**
     * Incorpora los eventos que todavía no están persistidos.
     * Los eventos históricos existentes no se eliminan ni se reemplazan.
     */
    private void synchronizeHistory(
            BookingEntity entity,
            java.util.List<StatusHistoryEntry> history) {

        Set<HistoryKey> existingKeys = new HashSet<>();

        for (BookingStatusHistoryEntity entry : entity.getHistory()) {
            existingKeys.add(new HistoryKey(
                    entry.getFromStatus(),
                    entry.getToStatus(),
                    entry.getActorUserId(),
                    entry.getReason(),
                    entry.getOccurredAt()));
        }

        for (StatusHistoryEntry entry : history) {
            HistoryKey key = new HistoryKey(
                    entry.from(),
                    entry.to(),
                    entry.actorUserId(),
                    entry.reason(),
                    entry.occurredAt());

            if (existingKeys.add(key)) {
                BookingStatusHistoryEntity historyEntity =
                        new BookingStatusHistoryEntity();

                historyEntity.setFromStatus(entry.from());
                historyEntity.setToStatus(entry.to());
                historyEntity.setActorUserId(entry.actorUserId());
                historyEntity.setReason(entry.reason());
                historyEntity.setOccurredAt(entry.occurredAt());

                entity.addHistory(historyEntity);
            }
        }
    }

    /**
     * Identifica un evento histórico mediante sus datos de negocio.
     */
    private record HistoryKey(
            String fromStatus,
            String toStatus,
            String actorUserId,
            String reason,
            LocalDateTime occurredAt) {
    }
}
