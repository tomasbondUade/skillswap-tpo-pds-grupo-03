package ar.edu.uade.skillswap.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryRepositoriesTest {
    @Test
    void storesProfilesAndPublications() {
        var profiles = new InMemoryProfileRepository();
        Profile profile = Profile.register("user-1", "Ana", "Bio", "UTC", Set.of("Java"), Set.of(), Set.of(), List.of());
        profiles.save(profile);

        assertEquals("Ana", profiles.findByUserId("user-1").orElseThrow().displayName());

        var publications = new InMemoryPublicationRepository();
        Publication publication = Publication.builder("user-1")
                .title("Java para principiantes")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(20))
                .availability(List.of(slot()))
                .build();
        publication.publish("user-1");
        publications.save(publication);

        assertEquals(publication.id(), publications.findById(publication.id()).orElseThrow().id());
        assertEquals(1, publications.findByAuthorUserId("user-1").size());

        PageResult<Publication> result = publications.searchPublished(new SearchQuery("Java", null, null, null, null, null, null, 0, 10));
        assertEquals(1L, result.totalElements());
        assertTrue(result.items().stream().anyMatch(item -> item.title().equals("Java para principiantes")));
    }

    private static AvailabilitySlot slot() {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        return AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2);
    }
}
