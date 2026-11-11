package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActorAndPublicationTypeMapperTest {

    @Test
    void preservesActorTypeAcrossEntityRoundTrip() {
        Profile organization = Profile.register("org-1", ActorType.ORGANIZATION, "Acme", "Empresa", "UTC",
                List.of(), List.of(), Set.of(NotificationChannelType.IN_APP), List.of());

        ProfileEntity entity = ProfileMapper.toEntity(organization);

        assertEquals("ORGANIZATION", entity.getActorType());
        assertEquals(ActorType.ORGANIZATION, ProfileMapper.toDomain(entity).actorType());
    }

    @Test
    void preservesPublicationTypeAcrossEntityRoundTrip() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 12, 1, 10, 0);
        Publication challenge = Publication.builder("org-1", ActorType.ORGANIZATION, PublicationType.CHALLENGE)
                .title("Hackatón de datos")
                .description("Desafío abierto")
                .category("Tecnología")
                .modality(Modality.HYBRID)
                .durationMinutes(120)
                .capacity(10)
                .basePrice(new SkillCoins(50))
                .addAvailability(AvailabilitySlot.of(startsAt, startsAt.plusHours(2), 10))
                .build();

        PublicationEntity entity = PublicationMapper.toEntity(challenge);

        assertEquals("CHALLENGE", entity.getType());
        assertEquals(PublicationType.CHALLENGE, PublicationMapper.toDomain(entity).type());
    }
}
