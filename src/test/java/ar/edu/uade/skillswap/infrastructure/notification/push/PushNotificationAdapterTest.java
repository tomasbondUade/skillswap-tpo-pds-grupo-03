package ar.edu.uade.skillswap.infrastructure.notification.push;

import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PushNotificationAdapterTest {

    @Test
    void enviaLosDatosCorrectosAlClientePush() {
        // Arrange: cliente de prueba que registra la llamada.
        class RecordingPushClient extends MockPushClient {
            String recipient;
            String title;
            String message;

            @Override
            public void sendPush(String recipient, String title, String message) {
                this.recipient = recipient;
                this.title = title;
                this.message = message;
            }
        }

        RecordingPushClient client = new RecordingPushClient();
        PushNotificationAdapter adapter = new PushNotificationAdapter(client);

        NotificationMessage notification = new NotificationMessage(
                "user-123",
                "Nueva reserva",
                "Tenés una nueva solicitud",
                "BOOKING_CREATED",
                NotificationChannelType.PUSH
        );

        // Act: enviamos la notificación al Adapter.
        adapter.send(notification);

        // Assert: comprobamos los datos recibidos por el cliente.
        assertEquals("user-123", client.recipient);
        assertEquals("Nueva reserva", client.title);
        assertEquals("Tenés una nueva solicitud", client.message);
    }

    @Test
    void rechazaUnaNotificacionQueNoEsPush() {
        PushNotificationAdapter adapter =
                new PushNotificationAdapter(new MockPushClient());

        NotificationMessage notification = new NotificationMessage(
                "user-123",
                "Nueva reserva",
                "Tenés una nueva solicitud",
                "BOOKING_CREATED",
                NotificationChannelType.EMAIL
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.send(notification)
        );
    }
}
