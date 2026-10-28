
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationEntity publication;

    @Column(name = "buyer_user_id", length = 100, nullable = false)
    private String buyerUserId;

    @Column(name = "provider_user_id", length = 100, nullable = false)
    private String providerUserId;

    @Column(name = "objective", length = 1000, nullable = false)
    private String objective;

    @Column(name = "slot_id", length = 100, nullable = false)
    private String slotId;

    @Column(name = "proposed_slot_id", length = 100)
    private String proposedSlotId;

    @Column(name = "buyer_accepted_reschedule", nullable = false)
    private boolean buyerAcceptedReschedule;

    @Column(name = "provider_accepted_reschedule", nullable = false)
    private boolean providerAcceptedReschedule;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "refund", nullable = false)
    private int refund;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BookingStatusHistoryEntity> history = new ArrayList<>();

    public BookingEntity() {
    }

    public String getId() { return id; }
    public PublicationEntity getPublication() { return publication; }
    public String getBuyerUserId() { return buyerUserId; }
    public String getProviderUserId() { return providerUserId; }
    public String getObjective() { return objective; }
    public String getSlotId() { return slotId; }
    public String getProposedSlotId() { return proposedSlotId; }
    public boolean isBuyerAcceptedReschedule() { return buyerAcceptedReschedule; }
    public boolean isProviderAcceptedReschedule() { return providerAcceptedReschedule; }
    public int getPrice() { return price; }
    public int getRefund() { return refund; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public List<BookingStatusHistoryEntity> getHistory() { return history; }

    public void setId(String id) { this.id = id; }
    public void setPublication(PublicationEntity publication) { this.publication = publication; }
    public void setBuyerUserId(String buyerUserId) { this.buyerUserId = buyerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public void setObjective(String objective) { this.objective = objective; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public void setProposedSlotId(String proposedSlotId) { this.proposedSlotId = proposedSlotId; }
    public void setBuyerAcceptedReschedule(boolean accepted) { this.buyerAcceptedReschedule = accepted; }
    public void setProviderAcceptedReschedule(boolean accepted) { this.providerAcceptedReschedule = accepted; }
    public void setPrice(int price) { this.price = price; }
    public void setRefund(int refund) { this.refund = refund; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public void addHistory(BookingStatusHistoryEntity entry) {
        history.add(entry);
        entry.setBooking(this);
    }
}
