package ar.edu.uade.skillswap.domain.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Perfil profesional: concentra datos, preferencias y reglas propias de su titular. */
public final class Profile {
    private record RatingIdentity(String bookingId, String authorUserId) {}

    private final String id;
    private final String userId;
    private String displayName;
    private String biography;
    private String timeZone;
    private final Set<String> skills;
    private final Set<String> interests;
    private final Set<NotificationChannelType> notificationChannels;
    private final List<AvailabilitySlot> availability;
    private double reputation;
    private int ratingCount;
    private boolean active;

    /** Constructor de reconstitución para adapters de persistencia. */
    public Profile(String id, String userId, String displayName, String biography, String timeZone,
                   Collection<String> skills, Collection<String> interests,
                   Collection<NotificationChannelType> notificationChannels, double reputation,
                   int ratingCount, boolean active) {
        this(id, userId, displayName, biography, timeZone, skills, interests,
                notificationChannels, List.of(), reputation, ratingCount, active);
    }

    /** Constructor de reconstitución que también recibe la disponibilidad persistida. */
    public Profile(String id, String userId, String displayName, String biography, String timeZone,
                   Collection<String> skills, Collection<String> interests,
                   Collection<NotificationChannelType> notificationChannels,
                   Collection<AvailabilitySlot> availability, double reputation,
                   int ratingCount, boolean active) {
        this.id = id == null ? UUID.randomUUID().toString() : required(id, "Falta identificador de perfil");
        this.userId = required(userId, "userId es obligatorio");
        this.displayName = required(displayName, "El nombre visible es obligatorio");
        this.biography = biography == null ? "" : biography.trim();
        this.timeZone = required(timeZone, "La zona horaria es obligatoria");
        this.skills = normalizedValues(skills, "habilidad");
        this.interests = normalizedValues(interests, "interés");
        this.notificationChannels = normalizedChannels(notificationChannels);
        this.availability = new ArrayList<>(availability == null ? List.of() : availability);
        validateAvailability(this.availability);
        if (!Double.isFinite(reputation) || reputation < 0 || reputation > 5 || ratingCount < 0) {
            throw new IllegalArgumentException("La reputación o la cantidad de calificaciones es inválida");
        }
        if (ratingCount == 0 && reputation != 0) {
            throw new IllegalArgumentException("Un perfil sin calificaciones debe tener reputación cero");
        }
        this.reputation = reputation;
        this.ratingCount = ratingCount;
        this.active = active;
    }

    public static Profile register(String userId, String name, String biography, String timeZone,
                                   Collection<String> skills, Collection<String> interests,
                                   Collection<NotificationChannelType> channels) {
        return register(userId, name, biography, timeZone, skills, interests, channels, List.of());
    }

    public static Profile register(String userId, String name, String biography, String timeZone,
                                   Collection<String> skills, Collection<String> interests,
                                   Collection<NotificationChannelType> channels,
                                   Collection<AvailabilitySlot> availability) {
        return new Profile(null, userId, name, biography, timeZone, skills, interests,
                channels, availability, 0, 0, true);
    }

    public void updateDetails(String actor, String name, String bio, String zone,
                              Collection<String> newSkills, Collection<String> newInterests) {
        requireActiveOwner(actor);
        String updatedName = required(name, "El nombre visible es obligatorio");
        String updatedZone = required(zone, "La zona horaria es obligatoria");
        Set<String> updatedSkills = normalizedValues(newSkills, "habilidad");
        Set<String> updatedInterests = normalizedValues(newInterests, "interés");
        displayName = updatedName;
        biography = bio == null ? "" : bio.trim();
        timeZone = updatedZone;
        skills.clear();
        skills.addAll(updatedSkills);
        interests.clear();
        interests.addAll(updatedInterests);
    }

    public void updateAvailability(String actor, Collection<AvailabilitySlot> slots) {
        requireActiveOwner(actor);
        List<AvailabilitySlot> updated = new ArrayList<>(slots == null ? List.of() : slots);
        validateAvailability(updated);
        availability.clear();
        availability.addAll(updated);
    }

    public void setNotificationChannels(String actor, Collection<NotificationChannelType> channels) {
        requireActiveOwner(actor);
        Set<NotificationChannelType> updated = normalizedChannels(channels);
        notificationChannels.clear();
        notificationChannels.addAll(updated);
    }

    /** La reputación se deriva de calificaciones verificadas, no se asigna manualmente. */
    public void recalculateReputation(Collection<Rating> verifiedRatings) {
        Collection<Rating> ratings = verifiedRatings == null ? List.of() : verifiedRatings;
        Set<RatingIdentity> identities = new HashSet<>();
        for (Rating rating : ratings) {
            if (rating == null || !userId.equals(rating.subjectUserId())) {
                throw new IllegalArgumentException("Todas las calificaciones deben pertenecer a este perfil");
            }
            RatingIdentity identity = new RatingIdentity(rating.bookingId(), rating.authorUserId());
            if (!identities.add(identity)) {
                throw new IllegalArgumentException("Una persona no puede calificar dos veces la misma contratación");
            }
        }
        if (ratings.isEmpty()) {
            reputation = 0;
            ratingCount = 0;
            return;
        }
        double average = ratings.stream().mapToInt(Rating::score).average().orElseThrow();
        reputation = Math.round(average * 10.0) / 10.0;
        ratingCount = ratings.size();
    }

    public void deactivate(String actor) {
        requireActiveOwner(actor);
        // La baja solo cambia el estado del perfil; las contrataciones tienen su propio ciclo de vida.
        active = false;
    }

    public void activate(String actor) {
        requireOwner(actor);
        if (active) throw new IllegalStateException("El perfil ya está activo");
        active = true;
    }

    private void requireActiveOwner(String actor) {
        requireOwner(actor);
        if (!active) throw new IllegalStateException("El perfil está dado de baja");
    }

    private void requireOwner(String actor) {
        if (!userId.equals(actor)) throw new SecurityException("Solo el titular puede modificar su perfil");
    }

    private static Set<String> normalizedValues(Collection<String> values, String label) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) return normalized;
        for (String value : values) normalized.add(required(value, "No puede haber " + label + " vacío/a").trim());
        return normalized;
    }

    private static Set<NotificationChannelType> normalizedChannels(Collection<NotificationChannelType> channels) {
        Set<NotificationChannelType> normalized = new LinkedHashSet<>();
        if (channels != null) {
            for (NotificationChannelType channel : channels) normalized.add(Objects.requireNonNull(channel));
        }
        if (normalized.isEmpty()) normalized.add(NotificationChannelType.IN_APP);
        return normalized;
    }

    private static void validateAvailability(List<AvailabilitySlot> slots) {
        Set<String> identifiers = new LinkedHashSet<>();
        for (int index = 0; index < slots.size(); index++) {
            AvailabilitySlot slot = Objects.requireNonNull(slots.get(index), "La disponibilidad no puede contener nulos");
            if (!identifiers.add(slot.id())) throw new IllegalArgumentException("La franja no puede repetirse");
            for (int otherIndex = index + 1; otherIndex < slots.size(); otherIndex++) {
                AvailabilitySlot other = Objects.requireNonNull(slots.get(otherIndex), "La disponibilidad no puede contener nulos");
                if (slot.startsAt().isBefore(other.endsAt()) && other.startsAt().isBefore(slot.endsAt())) {
                    throw new IllegalArgumentException("Las franjas del perfil no pueden superponerse");
                }
            }
        }
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public String id() { return id; }
    public String userId() { return userId; }
    public String displayName() { return displayName; }
    public String biography() { return biography; }
    public String timeZone() { return timeZone; }
    public Set<String> skills() { return Collections.unmodifiableSet(new LinkedHashSet<>(skills)); }
    public Set<String> interests() { return Collections.unmodifiableSet(new LinkedHashSet<>(interests)); }
    public Set<NotificationChannelType> notificationChannels() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(notificationChannels));
    }
    public List<AvailabilitySlot> availability() { return List.copyOf(availability); }
    public double reputation() { return reputation; }
    public int ratingCount() { return ratingCount; }
    public boolean active() { return active; }
}
