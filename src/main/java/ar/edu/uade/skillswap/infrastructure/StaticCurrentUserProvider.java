package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import java.util.Objects;

/** Infrastructure implementation injecting a fixed authenticated user id for the mock/demo layer. */
public final class StaticCurrentUserProvider implements CurrentUserProvider {
    private final String userId;

    public StaticCurrentUserProvider(String userId) {
        this.userId = Objects.requireNonNull(userId, "Falta el id del usuario autenticado");
    }

    @Override
    public String currentUserId() {
        return userId;
    }
}
