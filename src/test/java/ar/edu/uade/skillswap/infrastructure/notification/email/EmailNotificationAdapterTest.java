package ar.edu.uade.skillswap.infrastructure.notification.email;

import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailNotificationAdapterTest {

    @Test
    void enviaLosDatosCorrectosAlClienteEmail() {
        // Arrange: cliente de prueba que registra la llamada.
        class RecordingEmailClient extends MockEmailClient {
            String recipient;
            String subject;
            String body;

            @Override
            public void sendEmail(String recipient, String subject, String body) {
                this.recipient = recipient;
                this.subject = subject;
                this.body = body;
            }
        }

        RecordingEmailClient client = new RecordingEmailClient();
        EmailNotificationAdapter adapter = new EmailNotificationAdapter(client);

        NotificationMessage message = new NotificationMessage(
                "julieta@example.com",
                "Nueva reserva",
                "Tenés una nueva solicitud",
                "BOOKING_CREATED",
                NotificationChannelType.EMAIL
        );

        // Act: enviamos la notificación al Adapter.
        adapter.send(message);

        // Assert: el cliente recibió los tres argumentos esperados.
        assertEquals("julieta@example.com", client.recipient);
        assertEquals("Nueva reserva", client.subject);
        assertEquals("Tenés una nueva solicitud", client.body);
    }

    @Test
    void rechazaUnaNotificacionQueNoEsEmail() {
        EmailNotificationAdapter adapter =
                new EmailNotificationAdapter(new MockEmailClient());

        NotificationMessage message = new NotificationMessage(
                "user-123",
                "Nueva reserva",
                "Tenés una nueva solicitud",
                "BOOKING_CREATED",
                NotificationChannelType.PUSH
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.send(message)
        );
    }
}
