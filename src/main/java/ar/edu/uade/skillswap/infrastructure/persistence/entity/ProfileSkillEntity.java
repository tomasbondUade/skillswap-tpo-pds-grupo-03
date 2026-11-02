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
@Table(name = "profile_skills")
public class ProfileSkillEntity {

    @EmbeddedId
    private ProfileSkillId id;

    @MapsId("profileId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    public ProfileSkillEntity() {
    }

    public ProfileSkillId getId() {
        return id;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setId(ProfileSkillId id) {
        this.id = id;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    @Embeddable
    public static class ProfileSkillId implements Serializable {

        @Column(name = "profile_id", length = 100, nullable = false)
        private String profileId;

        @Column(name = "skill", length = 150, nullable = false)
        private String skill;

        protected ProfileSkillId() {
        }

        public ProfileSkillId(String profileId, String skill) {
            this.profileId = profileId;
            this.skill = skill;
        }

        public String getProfileId() {
            return profileId;
        }

        public String getSkill() {
            return skill;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProfileSkillId that)) {
                return false;
            }
            return Objects.equals(profileId, that.profileId)
                    && Objects.equals(skill, that.skill);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profileId, skill);
        }
    }
}