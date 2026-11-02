package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.infrastructure.notification.NotificationFactory;

import java.util.List;
import java.util.Objects;

public final class NotificationEventObserver {
    private final ProfileRepository profiles;
    private final NotificationFactory factory;

    public NotificationEventObserver(ProfileRepository profiles, NotificationFactory factory) {
        this.profiles = Objects.requireNonNull(profiles);
        this.factory = Objects.requireNonNull(factory);
    }

    public void onEvent(DomainEvent event) {
        String title = titleFor(event.name());
        String detail = event.attributes().getOrDefault("detail", title);
        List.of("buyerUserId", "providerUserId").stream()
                .map(event.attributes()::get)
                .filter(recipient -> recipient != null && !recipient.equals(event.actorUserId()))
                .distinct()
                .forEach(recipient -> send(event, recipient, title, detail));
    }

    private void send(DomainEvent event, String recipient, String title, String detail) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        var profile = profiles.findByUserId(recipient)
                .orElseThrow(() -> new IllegalStateException("No existe el perfil destinatario"));
        for (NotificationChannelType channel : profile.notificationChannels()) {
            factory.create(channel).send(new NotificationMessage(recipient, title, detail,
                    event.name(), channel));
        }
    }

    private static String titleFor(String eventName) {
        return switch (eventName) {
            case "booking.requested" -> "Nueva solicitud de contratación";
            case "booking.accepted" -> "Solicitud aceptada";
            case "booking.rejected" -> "Solicitud rechazada";
            case "booking.paid" -> "Pago confirmado";
            case "booking.reschedule-proposed" -> "Nueva propuesta de horario";
            case "booking.reschedule-accepted" -> "Propuesta de horario aceptada";
            case "booking.rescheduled" -> "Horario reprogramado";
            case "booking.cancelled" -> "Contratación cancelada";
            case "booking.started" -> "Sesión iniciada";
            case "booking.completed" -> "Sesión completada";
            default -> "Actualización de SkillSwap";
        };
    }
}
