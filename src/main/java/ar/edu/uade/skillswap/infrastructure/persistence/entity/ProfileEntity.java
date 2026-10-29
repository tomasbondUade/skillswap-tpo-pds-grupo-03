
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profiles")
public class ProfileEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @Column(name = "user_id", length = 100, nullable = false, unique = true)
    private String userId;

    @Column(name = "display_name", length = 150, nullable = false)
    private String displayName;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "time_zone", length = 100, nullable = false)
    private String timeZone;

    @Column(name = "reputation", precision = 3, scale = 2, nullable = false)
    private BigDecimal reputation;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProfileSkillEntity> skills = new ArrayList<>();

    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProfileInterestEntity> interests = new ArrayList<>();

    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProfileNotificationChannelEntity> notificationChannels =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProfileAvailabilityEntity> availability = new ArrayList<>();

    public ProfileEntity() {
        // Constructor requerido por JPA.
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBiography() {
        return biography;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public BigDecimal getReputation() {
        return reputation;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public boolean isActive() {
        return active;
    }

    public List<ProfileSkillEntity> getSkills() {
        return skills;
    }

    public List<ProfileInterestEntity> getInterests() {
        return interests;
    }

    public List<ProfileNotificationChannelEntity> getNotificationChannels() {
        return notificationChannels;
    }

    public List<ProfileAvailabilityEntity> getAvailability() {
        return availability;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public void setReputation(BigDecimal reputation) {
        this.reputation = reputation;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addSkill(ProfileSkillEntity skill) {
        skills.add(skill);
        skill.setProfile(this);
    }

    public void addInterest(ProfileInterestEntity interest) {
        interests.add(interest);
        interest.setProfile(this);
    }

    public void addNotificationChannel(
            ProfileNotificationChannelEntity channel) {
        notificationChannels.add(channel);
        channel.setProfile(this);
    }

    public void addAvailability(ProfileAvailabilityEntity slot) {
        availability.add(slot);
        slot.setProfile(this);
    }
}
