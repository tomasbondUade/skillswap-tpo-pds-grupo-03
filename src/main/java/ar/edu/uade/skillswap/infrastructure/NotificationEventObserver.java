package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;

import java.util.List;

public final class NotificationEventObserver {
    private final NotificationGateway gateway;

    public NotificationEventObserver(NotificationGateway gateway) {
        this.gateway = gateway;
    }

    public void onEvent(DomainEvent event) {
        String title = titleFor(event.name());
        String detail = event.attributes().getOrDefault("detail", title);
        if ("rating.submitted".equals(event.name())) {
            send(event, event.attributes().get("subjectUserId"), title, detail);
            return;
        }
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
        gateway.send(new NotificationMessage(recipient, title, detail,
                event.name(), NotificationChannelType.IN_APP));
    }

    private static String titleFor(String eventName) {
        return switch (eventName) {
            case "booking.requested" -> "Nueva solicitud de contratación";
            case "booking.accepted" -> "Solicitud aceptada";
            case "booking.rejected" -> "Solicitud rechazada";
            case "booking.paid" -> "Pago confirmado";
            case "booking.reschedule-proposed" -> "Nueva propuesta de horario";
            case "booking.rescheduled" -> "Horario reprogramado";
            case "booking.cancelled" -> "Contratación cancelada";
            case "booking.started" -> "Sesión iniciada";
            case "booking.completed" -> "Sesión completada";
            case "rating.submitted" -> "Nueva calificación recibida";
            default -> "Actualización de SkillSwap";
        };
    }
}