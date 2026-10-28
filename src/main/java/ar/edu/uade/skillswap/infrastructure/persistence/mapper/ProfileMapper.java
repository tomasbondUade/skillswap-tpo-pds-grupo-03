
package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileAvailabilityEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileInterestEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileNotificationChannelEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileSkillEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte entre el modelo de dominio Profile
 * y la entidad de persistencia ProfileEntity.
 */
public final class ProfileMapper {

    private ProfileMapper() {
        // Evita instanciar esta clase.
    }

    public static ProfileEntity toEntity(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException(
                    "El perfil no puede ser null"
            );
        }

        ProfileEntity entity = new ProfileEntity();

        entity.setId(profile.id());
        entity.setUserId(profile.userId());
        entity.setDisplayName(profile.displayName());
        entity.setBiography(profile.biography());
        entity.setTimeZone(profile.timeZone());
        entity.setReputation(BigDecimal.valueOf(profile.reputation()));
        entity.setRatingCount(profile.ratingCount());
        entity.setActive(profile.active());

        for (String skill : profile.skills()) {
            ProfileSkillEntity.ProfileSkillId id =
                    new ProfileSkillEntity.ProfileSkillId(
                            profile.id(), skill
                    );

            ProfileSkillEntity skillEntity = new ProfileSkillEntity();
            skillEntity.setId(id);
            entity.addSkill(skillEntity);
        }

        for (String interest : profile.interests()) {
            ProfileInterestEntity.ProfileInterestId id =
                    new ProfileInterestEntity.ProfileInterestId(
                            profile.id(), interest
                    );

            ProfileInterestEntity interestEntity =
                    new ProfileInterestEntity();

            interestEntity.setId(id);
            entity.addInterest(interestEntity);
        }

        for (NotificationChannelType channel
                : profile.notificationChannels()) {

            ProfileNotificationChannelEntity.ProfileNotificationChannelId id =
                    new ProfileNotificationChannelEntity
                            .ProfileNotificationChannelId(
                                    profile.id(), channel.name()
                            );

            ProfileNotificationChannelEntity channelEntity =
                    new ProfileNotificationChannelEntity();

            channelEntity.setId(id);
            entity.addNotificationChannel(channelEntity);
        }

        for (AvailabilitySlot slot : profile.availability()) {
            ProfileAvailabilityEntity slotEntity =
                    new ProfileAvailabilityEntity();

            slotEntity.setId(slot.id());
            slotEntity.setStartsAt(slot.startsAt());
            slotEntity.setEndsAt(slot.endsAt());
            slotEntity.setCapacity(slot.capacity());
            slotEntity.setReserved(slot.reserved());

            entity.addAvailability(slotEntity);
        }

        return entity;
    }

    public static Profile toDomain(ProfileEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "La entidad del perfil no puede ser null"
            );
        }

        Set<String> skills = entity.getSkills()
                .stream()
                .map(item -> item.getId().getSkill())
                .collect(Collectors.toSet());

        Set<String> interests = entity.getInterests()
                .stream()
                .map(item -> item.getId().getInterest())
                .collect(Collectors.toSet());

        Set<NotificationChannelType> channels =
                entity.getNotificationChannels()
                        .stream()
                        .map(item -> NotificationChannelType.valueOf(
                                item.getId().getChannel()
                        ))
                        .collect(Collectors.toSet());

        List<AvailabilitySlot> availability =
                entity.getAvailability()
                        .stream()
                        .map(item -> new AvailabilitySlot(
                                item.getId(),
                                item.getStartsAt(),
                                item.getEndsAt(),
                                item.getCapacity(),
                                item.getReserved()
                        ))
                        .toList();

        return new Profile(
                entity.getId(),
                entity.getUserId(),
                entity.getDisplayName(),
                entity.getBiography(),
                entity.getTimeZone(),
                skills,
                interests,
                channels,
                availability,
                entity.getReputation().doubleValue(),
                entity.getRatingCount(),
                entity.isActive()
        );
    }
}
