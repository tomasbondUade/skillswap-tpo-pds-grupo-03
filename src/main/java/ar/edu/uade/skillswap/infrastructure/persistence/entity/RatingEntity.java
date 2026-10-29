
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
        name = "ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_rating_booking_author",
                        columnNames = {"booking_id", "author_user_id"}
                )
        }
)
public class RatingEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @Column(name = "author_user_id", length = 100, nullable = false)
    private String authorUserId;

    @Column(name = "subject_user_id", length = 100, nullable = false)
    private String subjectUserId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "comment", length = 2000, nullable = false)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RatingEntity() {
    }

    public String getId() {
        return id;
    }

    public BookingEntity getBooking() {
        return booking;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getSubjectUserId() {
        return subjectUserId;
    }

    public int getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBooking(BookingEntity booking) {
        this.booking = booking;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public void setSubjectUserId(String subjectUserId) {
        this.subjectUserId = subjectUserId;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
