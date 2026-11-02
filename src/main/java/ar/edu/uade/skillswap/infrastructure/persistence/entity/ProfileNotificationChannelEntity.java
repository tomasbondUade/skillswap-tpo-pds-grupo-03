package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "profile_notification_channels")
public class ProfileNotificationChannelEntity {

    @EmbeddedId
    private ProfileNotificationChannelId id;

    @MapsId("profileId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    public ProfileNotificationChannelEntity() {
    }

    public ProfileNotificationChannelId getId() {
        return id;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setId(ProfileNotificationChannelId id) {
        this.id = id;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    @Embeddable
    public static class ProfileNotificationChannelId implements Serializable {

        @Column(name = "profile_id", length = 100, nullable = false)
        private String profileId;

        @Column(name = "channel", length = 20, nullable = false)
        private String channel;

        protected ProfileNotificationChannelId() {
        }

        public ProfileNotificationChannelId(String profileId, String channel) {
            this.profileId = profileId;
            this.channel = channel;
        }

        public String getProfileId() {
            return profileId;
        }

        public String getChannel() {
            return channel;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProfileNotificationChannelId that)) {
                return false;
            }
            return Objects.equals(profileId, that.profileId)
                    && Objects.equals(channel, that.channel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profileId, channel);
        }
    }
}