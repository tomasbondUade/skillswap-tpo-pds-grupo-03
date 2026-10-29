
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "publication_availability",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_publication_availability_publication_id",
                        columnNames = {"publication_id", "id"}
                )
        }
)
public class PublicationAvailabilityEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationEntity publication;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    public PublicationAvailabilityEntity() {
        // Constructor requerido por JPA.
    }

    public String getId() {
        return id;
    }

    public PublicationEntity getPublication() {
        return publication;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getReserved() {
        return reserved;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPublication(PublicationEntity publication) {
        this.publication = publication;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }
}
