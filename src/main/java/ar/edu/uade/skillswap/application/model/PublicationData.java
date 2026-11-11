package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Commands and read models for publication use cases. */
public final class PublicationData {
    private PublicationData() {}

    /** {@code type} nulo significa que el autor no eligió tipo: se usa el predeterminado de su actor. */
    public record CreateDraftCommand(String title, String description, String category,
                                     Modality modality, int durationMinutes, int capacity,
                                     int priceInSkillCoins, Set<String> tags,
                                     List<ProfileData.AvailabilityInput> availability, PublicationType type) {
        public CreateDraftCommand {
            tags = Set.copyOf(tags == null ? Set.of() : tags);
            availability = List.copyOf(availability == null ? List.of() : availability);
            if (priceInSkillCoins < 0) throw new IllegalArgumentException("El precio no puede ser negativo");
        }

        /** Borrador sin tipo elegido, como lo crea la UI actual. */
        public CreateDraftCommand(String title, String description, String category,
                                  Modality modality, int durationMinutes, int capacity,
                                  int priceInSkillCoins, Set<String> tags,
                                  List<ProfileData.AvailabilityInput> availability) {
            this(title, description, category, modality, durationMinutes, capacity, priceInSkillCoins, tags,
                    availability, null);
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

    /** Datos públicos del autor que acompañan a cada publicación. */
    public record AuthorView(String userId, String displayName, double reputation, int ratingCount) {}

    public record PublicationView(String id, String authorUserId, PublicationType type, String title,
                                  String description, String category, Modality modality,
                                  int durationMinutes, int capacity, int priceInSkillCoins,
                                  Set<String> tags, PublicationStatus status,
                                  List<ProfileData.AvailabilityView> availability,
                                  LocalDateTime createdAt, int requestCount, AuthorView author) {
        public PublicationView {
            tags = Set.copyOf(tags == null ? Set.of() : tags);
            availability = List.copyOf(availability == null ? List.of() : availability);
            Objects.requireNonNull(author, "Falta el autor de la publicación");
        }
    }
}
