package ar.edu.uade.skillswap.infrastructure.notification.push;

/**
 * Cliente simulado de notificaciones push.
 * Representa un servicio externo de envío de notificaciones.
 */
public class MockPushClient {

    public void sendPush(String recipient, String title, String message) {
        System.out.println(
                "[MOCK PUSH] Para: " + recipient
                        + " | Título: " + title
                        + " | Mensaje: " + message
        );
    }
}
