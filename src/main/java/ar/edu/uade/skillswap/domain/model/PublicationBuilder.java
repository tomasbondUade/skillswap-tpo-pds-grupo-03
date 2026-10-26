package ar.edu.uade.skillswap.domain.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Construye borradores de publicación paso a paso y delega las invariantes
 * del agregado en {@link Publication}.
 *
 * <p>Autor, título, descripción, categoría, modalidad, duración, cupo y precio
 * son obligatorios. Las etiquetas y franjas son opcionales para permitir crear
 * un borrador antes de definir su disponibilidad.</p>
 */
public final class PublicationBuilder {
    private final String authorUserId;
    private String title;
    private String description;
    private String category;
    private Modality modality;
    private Integer durationMinutes;
    private Integer capacity;
    private SkillCoins basePrice;
    private final Set<String> tags = new LinkedHashSet<>();
    private final List<AvailabilitySlot> availability = new ArrayList<>();

    PublicationBuilder(String authorUserId) {
        this.authorUserId = required(authorUserId, "Falta autor de publicación");
    }

    public PublicationBuilder title(String title) {
        this.title = title;
        return this;
    }

    public PublicationBuilder description(String description) {
        this.description = description;
        return this;
    }

    public PublicationBuilder category(String category) {
        this.category = category;
        return this;
    }

    public PublicationBuilder modality(Modality modality) {
        this.modality = modality;
        return this;
    }

    public PublicationBuilder durationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
        return this;
    }

    public PublicationBuilder capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public PublicationBuilder basePrice(SkillCoins basePrice) {
        this.basePrice = basePrice;
        return this;
    }

    public PublicationBuilder tag(String tag) {
        tags.add(required(tag, "Las etiquetas no pueden estar vacías"));
        return this;
    }

    public PublicationBuilder tags(Collection<String> tags) {
        this.tags.clear();
        if (tags != null) {
            for (String tag : tags) tag(tag);
        }
        return this;
    }

    public PublicationBuilder availability(Collection<AvailabilitySlot> slots) {
        availability.clear();
        if (slots != null) {
            for (AvailabilitySlot slot : slots) addAvailability(slot);
        }
        return this;
    }

    public PublicationBuilder addAvailability(AvailabilitySlot slot) {
        availability.add(Objects.requireNonNull(slot, "La disponibilidad no puede contener nulos"));
        return this;
    }

    /**
     * Construye el agregado una vez completos los datos requeridos.
     * Publication valida los límites, la consistencia de las franjas y el resto
     * de las reglas propias del dominio.
     */
    public Publication build() {
        requireText(title, "Falta título de publicación");
        requireText(description, "Falta descripción de publicación");
        requireText(category, "Falta categoría de publicación");
        Objects.requireNonNull(modality, "Falta modalidad");
        Objects.requireNonNull(durationMinutes, "Falta duración de publicación");
        Objects.requireNonNull(capacity, "Falta cupo de publicación");
        Objects.requireNonNull(basePrice, "Falta precio base");

        Publication publication = Publication.draft(authorUserId, title, description, category,
                modality, durationMinutes, capacity, basePrice, tags);
        for (AvailabilitySlot slot : availability) {
            publication.addAvailability(authorUserId, slot);
        }
        return publication;
    }

    private static void requireText(String value, String message) {
        required(value, message);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
