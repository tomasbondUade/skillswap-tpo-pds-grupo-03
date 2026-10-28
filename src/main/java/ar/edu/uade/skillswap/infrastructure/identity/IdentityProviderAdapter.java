
package ar.edu.uade.skillswap.infrastructure.identity;

import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;

/**
 * Adapta el cliente externo al contrato que necesita Application.
 */
public class IdentityProviderAdapter implements CurrentUserProvider {

    private final MockIdentityClient identityClient;

    public IdentityProviderAdapter(MockIdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    @Override
    public String currentUserId() {
        MockIdentityClient.IdentityResponse response =
                identityClient.getAuthenticatedIdentity();

        return response.subject();
    }
}
