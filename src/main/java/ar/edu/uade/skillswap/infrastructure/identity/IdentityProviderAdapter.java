
package ar.edu.uade.skillswap.infrastructure.identity;

import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import java.util.Objects;

/**
 * Adapta el cliente externo al contrato que necesita Application.
 */
public class IdentityProviderAdapter implements CurrentUserProvider {

    private final MockIdentityClient identityClient;

    public IdentityProviderAdapter(MockIdentityClient identityClient) {
        this.identityClient = Objects.requireNonNull(identityClient);
    }

    @Override
    public String currentUserId() {
        MockIdentityClient.IdentityResponse response =
                identityClient.getAuthenticatedIdentity();

        return response == null ? null : response.subject();
    }
}
