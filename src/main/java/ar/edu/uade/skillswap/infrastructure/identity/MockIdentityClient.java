
package ar.edu.uade.skillswap.infrastructure.identity;

/**
 * Cliente de identidad simulado.
 * Representa una API externa que devuelve un objeto,
 * en lugar de devolver directamente el ID como String.
 */
public class MockIdentityClient {

    private final String userId;

    public MockIdentityClient() {
        this("user-123");
    }

    public MockIdentityClient(String userId) {
        this.userId = userId;
    }

    public IdentityResponse getAuthenticatedIdentity() {
        return new IdentityResponse(userId);
    }

    public record IdentityResponse(String subject) {
    }
}
