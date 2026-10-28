package ar.edu.uade.skillswap.infrastructure.notification.email;

/**
 * Cliente simulado de email.
 * Representa un servicio externo con su propio contrato.
 */
public class MockEmailClient {

    public void sendEmail(String recipient, String subject, String body) {
        System.out.println(
                "[MOCK EMAIL] Para: " + recipient
                        + " | Asunto: " + subject
                        + " | Mensaje: " + body
        );
    }
}
