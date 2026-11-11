package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Profesionales que publican mentorías o servicios y organizaciones que publican desafíos (#38). */
class ActorPublicationTypeTest {
    private static final String AUTHOR = "author";
    private static final String OTHER = "other-user";

    @Test
    void eachActorTypeDefinesWhichPublicationTypesItCanCreate() {
        assertEquals(List.of(PublicationType.MENTORSHIP, PublicationType.SERVICE),
                List.copyOf(ActorType.PROFESSIONAL.publicationTypes()));
        assertEquals(List.of(PublicationType.CHALLENGE), List.copyOf(ActorType.ORGANIZATION.publicationTypes()));
        assertEquals(PublicationType.MENTORSHIP, ActorType.PROFESSIONAL.defaultPublicationType());
        assertEquals(PublicationType.CHALLENGE, ActorType.ORGANIZATION.defaultPublicationType());
        assertThrows(UnsupportedOperationException.class,
                () -> ActorType.PROFESSIONAL.publicationTypes().add(PublicationType.CHALLENGE));
    }

    @Test
    void everyPublicationTypeHasAnActorThatCanCreateIt() {
        for (PublicationType type : PublicationType.values()) {
            assertTrue(Arrays.stream(ActorType.values()).anyMatch(actor -> actor.canPublish(type)),
                    "Ningún actor puede crear " + type);
        }
    }

    @Test
    void anAuthorCreatesOnlyThePublicationTypesOfItsActorType() {
        for (ActorType actor : ActorType.values()) {
            for (PublicationType type : PublicationType.values()) {
                if (actor.canPublish(type)) {
                    assertEquals(type, ready(actor, type).type());
                    assertEquals(type, draft(actor, type).type());
                } else {
                    assertThrows(SecurityException.class, () -> ready(actor, type), actor + " no crea " + type);
                    assertThrows(SecurityException.class, () -> draft(actor, type), actor + " no crea " + type);
                }
            }
        }
    }

    @Test
    void registrationKeepsTheActorTypeAndDefaultsToProfessional() {
        Profile professional = Profile.register("ana", "Ana", "", "UTC", List.of("Java"), List.of(),
                Set.of(NotificationChannelType.IN_APP));
        Profile organization = Profile.register("acme", ActorType.ORGANIZATION, "Acme", "Empresa", "UTC",
                List.of(), List.of(), Set.of(NotificationChannelType.EMAIL), List.of());

        assertEquals(ActorType.PROFESSIONAL, professional.actorType());
        assertEquals(ActorType.ORGANIZATION, organization.actorType());
        assertTrue(organization.active());
        assertEquals(0, organization.reputation());
    }

    @Test
    void actorAndPublicationTypesAreRequired() {
        assertThrows(NullPointerException.class, () -> Profile.register("acme", null, "Acme", "", "UTC",
                List.of(), List.of(), Set.of(), List.of()));
        assertThrows(NullPointerException.class, () -> Publication.builder(AUTHOR, null, PublicationType.SERVICE));
        assertThrows(NullPointerException.class, () -> Publication.builder(AUTHOR, ActorType.PROFESSIONAL, null));
        assertThrows(NullPointerException.class, () -> draft(null, PublicationType.SERVICE));
        assertThrows(NullPointerException.class, () -> draft(ActorType.PROFESSIONAL, null));
        assertThrows(NullPointerException.class, () -> new Publication("p1", AUTHOR, null, "Java", "Mentoría",
                "Tecnología", Modality.REMOTE, 60, 1, new SkillCoins(10), Set.of(), List.of(),
                PublicationStates.draft(), LocalDateTime.now(), 0, null, 0, List.of()));
    }

    @ParameterizedTest
    @EnumSource(PublicationType.class)
    void authorManagesTheLifecycleOfEveryPublicationType(PublicationType type) {
        Publication publication = ready(creatorOf(type), type);
        publication.edit(AUTHOR, "Título editado", "Descripción", "Tecnología", Modality.HYBRID, 60, 2, Set.of());
        publication.publish(AUTHOR);
        publication.pause(AUTHOR);
        publication.publish(AUTHOR);
        publication.close(AUTHOR);

        assertEquals(PublicationStatus.CLOSED, publication.status());
        assertEquals(type, publication.type());
        assertEquals(5, publication.history().size());
        assertThrows(IllegalStateException.class, () -> publication.publish(AUTHOR));
    }

    @ParameterizedTest
    @EnumSource(PublicationType.class)
    void onlyTheAuthorCanEditOrChangeTheStatusOfEveryPublicationType(PublicationType type) {
        Publication draft = ready(creatorOf(type), type);
        assertThrows(SecurityException.class, () -> draft.edit(OTHER, "Otro", "Descripción", "Tecnología",
                Modality.REMOTE, 60, 2, Set.of()));
        assertThrows(SecurityException.class, () -> draft.publish(OTHER));
        assertThrows(SecurityException.class, () -> draft.cancel(OTHER, "Motivo"));

        Publication published = ready(creatorOf(type), type);
        published.publish(AUTHOR);
        assertThrows(SecurityException.class, () -> published.pause(OTHER));
        assertThrows(SecurityException.class, () -> published.close(OTHER));
        assertEquals(PublicationStatus.PUBLISHED, published.status());
    }

    @ParameterizedTest
    @EnumSource(PublicationType.class)
    void everyPublicationTypeRejectsTheSameInvalidTransitions(PublicationType type) {
        Publication draft = ready(creatorOf(type), type);
        assertThrows(IllegalStateException.class, () -> draft.pause(AUTHOR));
        assertThrows(IllegalStateException.class, () -> draft.close(AUTHOR));
        assertThrows(IllegalArgumentException.class, () -> draft.cancel(AUTHOR, " "));

        Publication cancelled = ready(creatorOf(type), type);
        cancelled.cancel(AUTHOR, "Retirada");
        assertThrows(IllegalStateException.class, () -> cancelled.publish(AUTHOR));
        assertEquals(PublicationStatus.CANCELLED, cancelled.status());
    }

    @Test
    void organizationCannotRequestItsOwnChallengeButOthersCan() {
        Publication challenge = ready(ActorType.ORGANIZATION, PublicationType.CHALLENGE);
        challenge.publish(AUTHOR);
        String slotId = challenge.availability().get(0).id();

        assertThrows(IllegalArgumentException.class, () -> challenge.recordBookingRequest(AUTHOR, slotId));
        assertDoesNotThrow(() -> challenge.recordBookingRequest("professional", slotId));
        assertEquals(1, challenge.requestCount());
    }

    private static ActorType creatorOf(PublicationType type) {
        return Arrays.stream(ActorType.values()).filter(actor -> actor.canPublish(type)).findFirst().orElseThrow();
    }

    private static Publication draft(ActorType actor, PublicationType type) {
        return Publication.draft(AUTHOR, actor, type, "Título", "Descripción", "Tecnología", Modality.REMOTE,
                60, 1, new SkillCoins(10), Set.of());
    }

    private static Publication ready(ActorType actor, PublicationType type) {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        return Publication.builder(AUTHOR, actor, type)
                .title("Título")
                .description("Descripción")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100))
                .addAvailability(AvailabilitySlot.of(start, start.plusHours(1), 2))
                .build();
    }
}
