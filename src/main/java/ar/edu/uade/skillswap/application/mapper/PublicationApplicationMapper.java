package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityInput;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityView;
import ar.edu.uade.skillswap.application.model.PublicationData.AuthorView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Converts publication aggregates to application read models. */
public final class PublicationApplicationMapper {
    private PublicationApplicationMapper() {}

    public static List<AvailabilitySlot> toAvailabilitySlots(Collection<AvailabilityInput> inputs) {
        if (inputs == null) return List.of();
        return inputs.stream()
                .map(input -> new AvailabilitySlot(input.id(), input.startsAt(), input.endsAt(), input.capacity(), 0))
                .toList();
    }

    /** Vista de la publicación con los datos públicos de su autor, que siempre tiene perfil. */
    public static PublicationView toView(Publication publication, Profile author) {
        Objects.requireNonNull(author, "Falta el perfil del autor de la publicación");
        List<AvailabilityView> availability = publication.availability().stream()
                .map(slot -> new AvailabilityView(slot.id(), slot.startsAt(), slot.endsAt(),
                        slot.capacity(), slot.reserved()))
                .toList();
        return new PublicationView(publication.id(), publication.authorUserId(), publication.title(),
                publication.description(), publication.category(), publication.modality(),
                publication.durationMinutes(), publication.capacity(), publication.basePrice().amount(),
                publication.tags(), publication.status(), availability, publication.createdAt(),
                publication.requestCount(), new AuthorView(author.userId(), author.displayName(),
                        author.reputation(), author.ratingCount()));
    }
}
