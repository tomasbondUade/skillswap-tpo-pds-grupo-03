package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publications")
public class PublicationEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @Column(name = "author_user_id", length = 100, nullable = false)
    private String authorUserId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "category", length = 150, nullable = false)
    private String category;

    @Column(name = "modality", length = 20, nullable = false)
    private String modality;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "base_price", nullable = false)
    private int basePrice;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "last_requested_at")
    private LocalDateTime lastRequestedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(
            mappedBy = "publication",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PublicationTagEntity> tags = new ArrayList<>();

    @OneToMany(
            mappedBy = "publication",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PublicationAvailabilityEntity> availability =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "publication",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PublicationStatusHistoryEntity> history =
            new ArrayList<>();

    public PublicationEntity() {
        // Constructor requerido por JPA.
    }

    public String getId() {
        return id;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getModality() {
        return modality;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public LocalDateTime getLastRequestedAt() {
        return lastRequestedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<PublicationTagEntity> getTags() {
        return tags;
    }

    public List<PublicationAvailabilityEntity> getAvailability() {
        return availability;
    }

    public List<PublicationStatusHistoryEntity> getHistory() {
        return history;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setBasePrice(int basePrice) {
        this.basePrice = basePrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public void setLastRequestedAt(LocalDateTime lastRequestedAt) {
        this.lastRequestedAt = lastRequestedAt;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void addTag(PublicationTagEntity tag) {
        tags.add(tag);
        tag.setPublication(this);
    }

    public void addAvailability(PublicationAvailabilityEntity slot) {
        availability.add(slot);
        slot.setPublication(this);
    }

    public void addHistory(PublicationStatusHistoryEntity entry) {
        history.add(entry);
        entry.setPublication(this);
    }
}