package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityInput;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityView;
import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.Collection;
import java.util.List;

/** Converts profile data at the Domain/Application boundary. */
public final class ProfileApplicationMapper {
    private ProfileApplicationMapper() {}

    public static List<AvailabilitySlot> toAvailabilitySlots(Collection<AvailabilityInput> inputs) {
        if (inputs == null) return List.of();
        return inputs.stream()
                .map(input -> new AvailabilitySlot(input.id(), input.startsAt(), input.endsAt(), input.capacity(), 0))
                .toList();
    }

    public static ProfileView toView(Profile profile) {
        List<AvailabilityView> availability = profile.availability().stream()
                .map(slot -> new AvailabilityView(slot.id(), slot.startsAt(), slot.endsAt(),
                        slot.capacity(), slot.reserved()))
                .toList();
        return new ProfileView(profile.id(), profile.userId(), profile.displayName(), profile.biography(),
                profile.timeZone(), List.copyOf(profile.skills()), List.copyOf(profile.interests()),
                profile.notificationChannels(), availability, profile.reputation(), profile.ratingCount(),
                profile.active());
    }
}
