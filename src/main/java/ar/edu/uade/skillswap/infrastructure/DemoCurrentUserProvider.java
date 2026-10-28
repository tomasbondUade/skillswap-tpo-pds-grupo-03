package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import java.util.Set;

/** Sesión intercambiable exclusivamente para la demo Swing, nunca para autenticación real. */
public final class DemoCurrentUserProvider implements CurrentUserProvider {
    private static final Set<String> USERS = Set.of("user-mateo", "mentor-ana", "mentor-lu");
    private String active = "user-mateo";

    @Override public String currentUserId() { return active; }

    public void switchTo(String userId) {
        if (!USERS.contains(userId)) throw new IllegalArgumentException("Usuario demo desconocido");
        active = userId;
    }
}
