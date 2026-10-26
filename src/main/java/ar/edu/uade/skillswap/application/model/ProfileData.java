package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Commands and read models for profile use cases. No domain entity crosses the application boundary. */
public final class ProfileData {
    private ProfileData() {}

    public record AvailabilityInput(String id, LocalDateTime startsAt, LocalDateTime endsAt, int capacity) {
        public AvailabilityInput {
            if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt) || capacity < 1) {
                throw new IllegalArgumentException("La franja de disponibilidad es inválida");
            }
        }
    }

    public record AvailabilityView(String id, LocalDateTime startsAt, LocalDateTime endsAt,
                                   int capacity, int reserved) {}

    public record RegisterCommand(String displayName, String biography, String timeZone,
                                  List<String> skills, List<String> interests,
                                  Set<NotificationChannelType> notificationChannels,
                                  List<AvailabilityInput> availability) {
        public RegisterCommand {
            skills = List.copyOf(skills == null ? List.of() : skills);
            interests = List.copyOf(interests == null ? List.of() : interests);
            notificationChannels = Set.copyOf(notificationChannels == null ? Set.of() : notificationChannels);
            availability = List.copyOf(availability == null ? List.of() : availability);
        }
    }

    public record UpdateCommand(String displayName, String biography, String timeZone,
                                List<String> skills, List<String> interests) {
        public UpdateCommand {
            skills = List.copyOf(skills == null ? List.of() : skills);
            interests = List.copyOf(interests == null ? List.of() : interests);
        }
    }

    public record ProfileView(String id, String userId, String displayName, String biography,
                              String timeZone, List<String> skills, List<String> interests,
                              Set<NotificationChannelType> notificationChannels,
                              List<AvailabilityView> availability, double reputation,
                              int ratingCount, boolean active) {
        public ProfileView {
            skills = List.copyOf(skills == null ? List.of() : skills);
            interests = List.copyOf(interests == null ? List.of() : interests);
            notificationChannels = Set.copyOf(notificationChannels == null ? Set.of() : notificationChannels);
            availability = List.copyOf(availability == null ? List.of() : availability);
        }
    }
}
