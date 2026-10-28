package ar.edu.uade.skillswap.infrastructure.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityProviderAdapterTest {

    @Test
    void devuelveElIdDelUsuarioAutenticado() {
        // Arrange: creamos el cliente simulado y el Adapter.
        MockIdentityClient client = new MockIdentityClient();
        IdentityProviderAdapter adapter = new IdentityProviderAdapter(client);

        // Act: solicitamos el identificador mediante el contrato de Application.
        String userId = adapter.currentUserId();

        // Assert: verificamos el identificador devuelto.
        assertEquals("user-123", userId);
    }
}
