
package ar.edu.uade.skillswap.infrastructure.identity;

/**
 * Cliente de identidad simulado.
 * Representa una API externa que devuelve un objeto,
 * en lugar de devolver directamente el ID como String.
 */
public class MockIdentityClient {

    public IdentityResponse getAuthenticatedIdentity() {
        return new IdentityResponse("user-123");
    }

    public record IdentityResponse(String subject) {
    }
}
