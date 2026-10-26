package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Commands and read models for publication use cases. */
public final class PublicationData {
    private PublicationData() {}

    public record CreateDraftCommand(String title, String description, String category,
                                     Modality modality, int durationMinutes, int capacity,
                                     int priceInSkillCoins, Set<String> tags,
                                     List<ProfileData.AvailabilityInput> availability) {
        public CreateDraftCommand {
            tags = Set.copyOf(tags == null ? Set.of() : tags);
            availability = List.copyOf(availability == null ? List.of() : availability);
            if (priceInSkillCoins < 0) throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }

    public record EditDraftCommand(String publicationId, String title, String description,
                                   String category, Modality modality, int durationMinutes,
                                   int capacity, Set<String> tags) {
        public EditDraftCommand {
            tags = Set.copyOf(tags == null ? Set.of() : tags);
        }
    }

    public record ChangeStatusCommand(String publicationId, PublicationStatus targetStatus,
                                      String reason) {}

    public record PublicationView(String id, String authorUserId, String title, String description,
                                  String category, Modality modality, int durationMinutes, int capacity,
                                  int priceInSkillCoins, Set<String> tags, PublicationStatus status,
                                  List<ProfileData.AvailabilityView> availability,
                                  LocalDateTime createdAt, int requestCount) {
        public PublicationView {
            tags = Set.copyOf(tags == null ? Set.of() : tags);
            availability = List.copyOf(availability == null ? List.of() : availability);
        }
    }
}
