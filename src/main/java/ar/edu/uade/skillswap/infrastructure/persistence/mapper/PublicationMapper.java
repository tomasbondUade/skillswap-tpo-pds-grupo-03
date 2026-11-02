
package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.StatusHistoryEntry;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationAvailabilityEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationStatusHistoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationTagEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PublicationMapper {

    private PublicationMapper() {
    }

    public static PublicationEntity toEntity(Publication publication) {
        if (publication == null) {
            throw new IllegalArgumentException("La publicación no puede ser null");
        }

        PublicationEntity entity = new PublicationEntity();
        entity.setId(publication.id());
        entity.setAuthorUserId(publication.authorUserId());
        entity.setTitle(publication.title());
        entity.setDescription(publication.description());
        entity.setCategory(publication.category());
        entity.setModality(publication.modality().name());
        entity.setDurationMinutes(publication.durationMinutes());
        entity.setCapacity(publication.capacity());
        entity.setBasePrice(publication.basePrice().amount());
        entity.setStatus(publication.status().name());
        entity.setCreatedAt(publication.createdAt());
        entity.setRequestCount(publication.requestCount());
        entity.setLastRequestedAt(publication.lastRequestedAt());
        entity.setVersion(publication.version());

        for (String tag : publication.tags()) {
            PublicationTagEntity tagEntity = new PublicationTagEntity();
            tagEntity.setId(
                    new PublicationTagEntity.PublicationTagId(publication.id(), tag)
            );
            entity.addTag(tagEntity);
        }

        for (AvailabilitySlot slot : publication.availability()) {
            PublicationAvailabilityEntity slotEntity =
                    new PublicationAvailabilityEntity();

            slotEntity.setId(slot.id());
            slotEntity.setStartsAt(slot.startsAt());
            slotEntity.setEndsAt(slot.endsAt());
            slotEntity.setCapacity(slot.capacity());
            slotEntity.setReserved(slot.reserved());

            entity.addAvailability(slotEntity);
        }

        for (StatusHistoryEntry entry : publication.history()) {
            PublicationStatusHistoryEntity historyEntity =
                    new PublicationStatusHistoryEntity();

            historyEntity.setFromStatus(entry.from());
            historyEntity.setToStatus(entry.to());
            historyEntity.setActorUserId(entry.actorUserId());
            historyEntity.setReason(entry.reason());
            historyEntity.setOccurredAt(entry.occurredAt());

            entity.addHistory(historyEntity);
        }

        return entity;
    }

    public static Publication toDomain(PublicationEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad no puede ser null");
        }

        Set<String> tags = entity.getTags().stream()
                .map(tag -> tag.getId().getTag())
                .collect(Collectors.toSet());

        List<AvailabilitySlot> availability = entity.getAvailability().stream()
                .map(slot -> new AvailabilitySlot(
                        slot.getId(),
                        slot.getStartsAt(),
                        slot.getEndsAt(),
                        slot.getCapacity(),
                        slot.getReserved()
                ))
                .toList();

        List<StatusHistoryEntry> history = entity.getHistory().stream()
                .map(entry -> new StatusHistoryEntry(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        entry.getActorUserId(),
                        entry.getReason(),
                        entry.getOccurredAt()
                ))
                .toList();

        PublicationStatus status = PublicationStatus.valueOf(entity.getStatus());

        return new Publication(
                entity.getId(),
                entity.getAuthorUserId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                Modality.valueOf(entity.getModality()),
                entity.getDurationMinutes(),
                entity.getCapacity(),
                new SkillCoins(entity.getBasePrice()),
                tags,
                availability,
                switch (status) {
                    case DRAFT -> PublicationStates.draft();
                    case PUBLISHED -> PublicationStates.published();
                    case PAUSED -> PublicationStates.paused();
                    case CLOSED -> PublicationStates.closed();
                    case CANCELLED -> PublicationStates.cancelled();
                },
                entity.getCreatedAt(),
                entity.getRequestCount(),
                entity.getLastRequestedAt(),
                entity.getVersion(),
                history
        );
    }
}
