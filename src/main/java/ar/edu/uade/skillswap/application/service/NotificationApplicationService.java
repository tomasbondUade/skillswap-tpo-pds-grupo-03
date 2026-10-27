package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.port.in.NotificationUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import java.util.List;
import java.util.Objects;

/** Orchestrates the authenticated user's inbox and read acknowledgements. */
public final class NotificationApplicationService implements NotificationUseCases {
    private final NotificationRepository notifications;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;

    public NotificationApplicationService(NotificationRepository notifications, CurrentUserProvider currentUser,
                                          TransactionManager transactions) {
        this.notifications = Objects.requireNonNull(notifications);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public List<NotificationView> listMine() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        return List.copyOf(notifications.findByRecipientUserId(actor));
    }

    @Override
    public void markRead(String notificationId) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        String id = UseCaseSupport.requiredId(notificationId, "notificación");
        transactions.required(() -> {
            notifications.findByIdAndRecipientUserId(id, actor)
                    .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                            "No existe una notificación para el usuario autenticado"));
            notifications.markRead(id, actor);
        });
    }

    @Override
    public void markAllRead() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        transactions.required(() -> notifications.markAllRead(actor));
    }
}
