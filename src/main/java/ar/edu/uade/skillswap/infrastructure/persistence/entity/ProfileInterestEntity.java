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
@Table(name = "profile_interests")
public class ProfileInterestEntity {

    @EmbeddedId
    private ProfileInterestId id;

    @MapsId("profileId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    public ProfileInterestEntity() {
    }

    public ProfileInterestId getId() {
        return id;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setId(ProfileInterestId id) {
        this.id = id;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    @Embeddable
    public static class ProfileInterestId implements Serializable {

        @Column(name = "profile_id", length = 100, nullable = false)
        private String profileId;

        @Column(name = "interest", length = 150, nullable = false)
        private String interest;

        protected ProfileInterestId() {
        }

        public ProfileInterestId(String profileId, String interest) {
            this.profileId = profileId;
            this.interest = interest;
        }

        public String getProfileId() {
            return profileId;
        }

        public String getInterest() {
            return interest;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProfileInterestId that)) {
                return false;
            }
            return Objects.equals(profileId, that.profileId)
                    && Objects.equals(interest, that.interest);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profileId, interest);
        }
    }
}