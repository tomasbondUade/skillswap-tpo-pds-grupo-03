
package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.StatusHistoryEntry;
import ar.edu.uade.skillswap.domain.state.BookingStates;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingStatusHistoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;

import java.util.List;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingEntity toEntity(Booking booking, PublicationEntity publication) {
        if (booking == null) {
            throw new IllegalArgumentException("La reserva no puede ser null");
        }
        if (publication == null) {
            throw new IllegalArgumentException("La publicación no puede ser null");
        }
        if (!booking.publicationId().equals(publication.getId())) {
            throw new IllegalArgumentException("La reserva y la publicación no coinciden");
        }

        BookingEntity entity = new BookingEntity();
        entity.setId(booking.id());
        entity.setPublication(publication);
        entity.setBuyerUserId(booking.buyerUserId());
        entity.setProviderUserId(booking.providerUserId());
        entity.setObjective(booking.objective());
        entity.setSlotId(booking.slotId());
        entity.setProposedSlotId(booking.proposedSlotId());
        entity.setBuyerAcceptedReschedule(booking.buyerAcceptedReschedule());
        entity.setProviderAcceptedReschedule(booking.providerAcceptedReschedule());
        entity.setPrice(booking.price().amount());
        entity.setRefund(booking.refund().amount());
        entity.setStatus(booking.status().name());
        entity.setCreatedAt(booking.createdAt());
        entity.setPaidAt(booking.paidAt());

        for (StatusHistoryEntry entry : booking.history()) {
            BookingStatusHistoryEntity historyEntity =
                    new BookingStatusHistoryEntity();

            historyEntity.setFromStatus(entry.from());
            historyEntity.setToStatus(entry.to());
            historyEntity.setActorUserId(entry.actorUserId());
            historyEntity.setReason(entry.reason());
            historyEntity.setOccurredAt(entry.occurredAt());

            entity.addHistory(historyEntity);
        }

        return entity;
    }

    public static Booking toDomain(BookingEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad de reserva no puede ser null");
        }

        List<StatusHistoryEntry> history = entity.getHistory().stream()
                .map(entry -> new StatusHistoryEntry(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        entry.getActorUserId(),
                        entry.getReason(),
                        entry.getOccurredAt()
                ))
                .toList();

        BookingStatus status = BookingStatus.valueOf(entity.getStatus());

        return new Booking(
                entity.getId(),
                entity.getPublication().getId(),
                entity.getBuyerUserId(),
                entity.getProviderUserId(),
                entity.getObjective(),
                entity.getSlotId(),
                new SkillCoins(entity.getPrice()),
                switch (status) {
                    case PENDING -> BookingStates.pending();
                    case ACCEPTED -> BookingStates.accepted();
                    case CONFIRMED -> BookingStates.confirmed();
                    case IN_PROGRESS -> BookingStates.inProgress();
                    case COMPLETED -> BookingStates.completed();
                    case REJECTED -> BookingStates.rejected();
                    case CANCELLED -> BookingStates.cancelled();
                },
                entity.getCreatedAt(),
                entity.getPaidAt(),
                new SkillCoins(entity.getRefund()),
                entity.getProposedSlotId(),
                entity.isBuyerAcceptedReschedule(),
                entity.isProviderAcceptedReschedule(),
                history
        );
    }
}
