package ar.edu.uade.skillswap.infrastructure.notification;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.DomainEventPublisher;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;

import java.util.Objects;

/**
 * Convierte el evento de una calificación en notificaciones
 * y selecciona el adaptador correspondiente a cada preferencia.
 */
public final class RatingNotificationEventPublisher implements DomainEventPublisher {

    private final ProfileRepository profiles;
    private final NotificationFactory notificationFactory;

    public RatingNotificationEventPublisher(
            ProfileRepository profiles,
            NotificationFactory notificationFactory) {

        this.profiles = Objects.requireNonNull(profiles);
        this.notificationFactory = Objects.requireNonNull(notificationFactory);
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "Falta el evento");

        // Este publicador solo procesa eventos de calificaciones.
        if (!"RatingSubmitted".equals(event.name())) {
            return;
        }

        String recipientUserId = event.attributes().get("recipientUserId");

        if (recipientUserId == null || recipientUserId.isBlank()) {
            throw new IllegalArgumentException(
                    "El evento no indica quién recibe la calificación");
        }

        Profile recipient = profiles.findByUserId(recipientUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el perfil destinatario de la calificación"));

        String score = event.attributes().get("score");
        String comment = event.attributes().get("comment");

        if (score == null || score.isBlank()) {
            throw new IllegalArgumentException(
                    "El evento no indica el puntaje de la calificación");
        }

        String title = "Recibiste una nueva calificación";
        String detail = "Recibiste una calificación de " + score + "/5.";

        if (comment != null && !comment.isBlank()) {
            detail += " Comentario: " + comment;
        }

        // Una notificación por cada canal configurado en el perfil.
        for (NotificationChannelType channel : recipient.notificationChannels()) {
            NotificationMessage message = new NotificationMessage(
                    recipient.userId(),
                    title,
                    detail,
                    "RATING_RECEIVED",
                    channel
            );

            notificationFactory.create(channel).send(message);
        }
    }
}
