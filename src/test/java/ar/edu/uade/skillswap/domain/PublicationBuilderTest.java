package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationBuilder;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicationBuilderTest {
    private static final String AUTHOR = "mentor-1";

    @Test
    void buildsDraftWithRequiredAndOptionalInformation() {
        LocalDateTime start = LocalDateTime.parse("2026-11-01T10:00:00");
        AvailabilitySlot slot = AvailabilitySlot.of(start, start.plusHours(1), 2);

        Publication publication = completeBuilder()
                .title("  Java avanzado  ")
                .tags(Set.of("Java", "Backend"))
                .addAvailability(slot)
                .build();

        assertEquals("Java avanzado", publication.title());
        assertEquals(AUTHOR, publication.authorUserId());
        assertEquals(Set.of("Java", "Backend"), publication.tags());
        assertEquals(List.of(slot), publication.availability());
        assertEquals(PublicationStatus.DRAFT, publication.status());
        publication.publish(AUTHOR);
        assertEquals(PublicationStatus.PUBLISHED, publication.status());
    }

    @Test
    void allowsOptionalTagsAndAvailabilityToBeOmittedForADraft() {
        Publication publication = completeBuilder().build();

        assertTrue(publication.tags().isEmpty());
        assertTrue(publication.availability().isEmpty());
        assertThrows(IllegalStateException.class, () -> publication.publish(AUTHOR));
    }

    @Test
    void requiresEveryMandatoryField() {
        assertThrows(IllegalArgumentException.class, () -> Publication.builder(" "));
        assertThrows(IllegalArgumentException.class, () -> completeBuilder().title(" ").build());
        assertThrows(IllegalArgumentException.class, () -> completeBuilder().description(null).build());
        assertThrows(IllegalArgumentException.class, () -> completeBuilder().category("").build());
        assertThrows(NullPointerException.class, () -> completeBuilder().modality(null).build());
        assertThrows(NullPointerException.class, () -> completeBuilder().basePrice(null).build());
        assertThrows(NullPointerException.class, () -> builderWithoutDuration().build());
        assertThrows(NullPointerException.class, () -> builderWithoutCapacity().build());
    }

    @Test
    void delegatesNumericLimitsAndAvailabilityInvariantsToPublication() {
        assertThrows(IllegalArgumentException.class,
                () -> completeBuilder().durationMinutes(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> completeBuilder().capacity(0).build());

        LocalDateTime start = LocalDateTime.parse("2026-11-01T10:00:00");
        AvailabilitySlot tooShort = AvailabilitySlot.of(start, start.plusMinutes(30), 1);
        assertThrows(IllegalArgumentException.class,
                () -> completeBuilder().addAvailability(tooShort).build());

        AvailabilitySlot overCapacity = AvailabilitySlot.of(start, start.plusHours(1), 3);
        assertThrows(IllegalArgumentException.class,
                () -> completeBuilder().addAvailability(overCapacity).build());

        AvailabilitySlot first = AvailabilitySlot.of(start, start.plusHours(1), 1);
        AvailabilitySlot overlapping = AvailabilitySlot.of(start.plusMinutes(30), start.plusHours(2), 1);
        assertThrows(IllegalArgumentException.class,
                () -> completeBuilder().availability(List.of(first, overlapping)).build());
    }

    @Test
    void rejectsBlankTagsAndNullAvailability() {
        assertThrows(IllegalArgumentException.class,
                () -> completeBuilder().tag(" "));
        assertThrows(NullPointerException.class,
                () -> completeBuilder().addAvailability(null));
    }

    private static PublicationBuilder completeBuilder() {
        return Publication.builder(AUTHOR)
                .title("Java avanzado")
                .description("Mentoría individual")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(100));
    }

    private static PublicationBuilder builderWithoutDuration() {
        return Publication.builder(AUTHOR)
                .title("Java avanzado")
                .description("Mentoría individual")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .capacity(2)
                .basePrice(new SkillCoins(100));
    }

    private static PublicationBuilder builderWithoutCapacity() {
        return Publication.builder(AUTHOR)
                .title("Java avanzado")
                .description("Mentoría individual")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .basePrice(new SkillCoins(100));
    }
}
