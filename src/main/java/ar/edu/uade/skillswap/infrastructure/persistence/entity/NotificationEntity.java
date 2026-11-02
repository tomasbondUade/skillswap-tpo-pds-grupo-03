
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @Column(name = "recipient_user_id", length = 100, nullable = false)
    private String recipientUserId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "detail", length = 2000, nullable = false)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "type", length = 100, nullable = false)
    private String type;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    public NotificationEntity() {
    }

    public String getId() {
        return id;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public String getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
