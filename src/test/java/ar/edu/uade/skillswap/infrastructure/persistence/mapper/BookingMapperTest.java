package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.BookingEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BookingMapperTest {

    @Test
    void preservesRequestDetailsAcrossEntityRoundTrip() {
        String clientMessage = "¿Podemos repasar ejercicios antes de la entrevista?";
        String objective = "Practicar diseño de APIs";
        LocalDateTime startsAt = LocalDateTime.of(2026, 11, 12, 10, 0);
        AvailabilitySlot slot = AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2);
        Publication publication = Publication.builder("mentor-1")
                .title("Mentoría de Java")
                .description("Práctica individual")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .addAvailability(slot)
                .build();
        publication.publish("mentor-1");
        Booking requested = Booking.request(publication, "student-1", clientMessage,
                objective, slot.id(), publication.basePrice());

        PublicationEntity publicationEntity = new PublicationEntity();
        publicationEntity.setId(publication.id());

        BookingEntity persistedShape = BookingMapper.toEntity(requested, publicationEntity);
        Booking restored = BookingMapper.toDomain(persistedShape);

        assertNotNull(persistedShape.getClientMessage());
        assertEquals(clientMessage, persistedShape.getClientMessage());
        assertEquals(clientMessage, restored.clientMessage());
        assertEquals(objective, restored.objective());
        assertEquals(slot.id(), restored.slotId());
    }

    @Test
    void treatsLegacyRowsWithoutClientMessageAsEmpty() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 12, 9, 0);
        PublicationEntity publication = new PublicationEntity();
        publication.setId("publication-1");

        BookingEntity legacyRow = new BookingEntity();
        legacyRow.setId("booking-1");
        legacyRow.setPublication(publication);
        legacyRow.setBuyerUserId("student-1");
        legacyRow.setProviderUserId("mentor-1");
        legacyRow.setObjective("Practicar diseño de APIs");
        legacyRow.setSlotId("slot-1");
        legacyRow.setPrice(100);
        legacyRow.setRefund(0);
        legacyRow.setStatus("PENDING");
        legacyRow.setCreatedAt(createdAt);

        Booking restored = BookingMapper.toDomain(legacyRow);

        assertEquals("", restored.clientMessage());
    }
}
