package ar.edu.uade.skillswap.infrastructure.persistence.repository;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import java.time.LocalDateTime;
import java.util.UUID;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.NotificationEntity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JpaNotificationRepository extends JpaRepositorySupport
        implements NotificationRepository {

    public JpaNotificationRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public List<NotificationView> findByRecipientUserId(String userId) {
        Objects.requireNonNull(
                userId, "Falta el identificador del destinatario");

        return query(em -> em.createQuery(
                        "SELECT n FROM NotificationEntity n "
                                + "WHERE n.recipientUserId = :userId "
                                + "ORDER BY n.createdAt DESC, n.id ASC",
                        NotificationEntity.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(JpaNotificationRepository::toView)
                .toList());
    }

    @Override
    public Optional<NotificationView> findByIdAndRecipientUserId(
            String notificationId,
            String userId) {

        Objects.requireNonNull(
                notificationId, "Falta el identificador de la notificación");
        Objects.requireNonNull(
                userId, "Falta el identificador del destinatario");

        return query(em -> em.createQuery(
                        "SELECT n FROM NotificationEntity n "
                                + "WHERE n.id = :notificationId "
                                + "AND n.recipientUserId = :userId",
                        NotificationEntity.class)
                .setParameter("notificationId", notificationId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(JpaNotificationRepository::toView));
    }

	
@Override
public void save(NotificationMessage notification) {
    Objects.requireNonNull(notification, "Falta la notificación");

    NotificationEntity entity = new NotificationEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setRecipientUserId(notification.recipientUserId());
    entity.setTitle(notification.title());
    entity.setDetail(notification.detail());
    entity.setCreatedAt(LocalDateTime.now());
    entity.setRead(false);
    entity.setType(notification.type());
    entity.setChannel(notification.channel().name());

    query(em -> {
        em.persist(entity);
        return null;
    });
}


    @Override
    public void markRead(String notificationId, String userId) {
        Objects.requireNonNull(
                notificationId, "Falta el identificador de la notificación");
        Objects.requireNonNull(
                userId, "Falta el identificador del destinatario");

        entityManager().createQuery(
                        "UPDATE NotificationEntity n "
                                + "SET n.read = true "
                                + "WHERE n.id = :notificationId "
                                + "AND n.recipientUserId = :userId")
                .setParameter("notificationId", notificationId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void markAllRead(String userId) {
        Objects.requireNonNull(
                userId, "Falta el identificador del destinatario");

        entityManager().createQuery(
                        "UPDATE NotificationEntity n "
                                + "SET n.read = true "
                                + "WHERE n.recipientUserId = :userId "
                                + "AND n.read = false")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private static NotificationView toView(NotificationEntity entity) {
        return new NotificationView(
                entity.getId(),
                entity.getTitle(),
                entity.getDetail(),
                entity.getCreatedAt(),
                entity.isRead(),
                entity.getType());
    }
}
