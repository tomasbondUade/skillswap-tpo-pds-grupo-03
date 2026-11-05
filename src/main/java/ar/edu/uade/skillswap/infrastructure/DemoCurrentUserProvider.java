package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import java.util.Set;

/** Sesión intercambiable exclusivamente para la demo Swing, nunca para autenticación real. */
public final class DemoCurrentUserProvider implements CurrentUserProvider {
    private static final Set<String> USERS = Set.of("user-mateo", "mentor-ana", "mentor-lu");
    private final CurrentUserProvider original;
    private String active;

    public DemoCurrentUserProvider() {
        this(() -> "user-mateo");
    }

    public DemoCurrentUserProvider(CurrentUserProvider original) {
        this.original = java.util.Objects.requireNonNull(original);
    }

    @Override public String currentUserId() { return active == null ? original.currentUserId() : active; }

    public void switchTo(String userId) {
        if (userId == null || !USERS.contains(userId)) throw new IllegalArgumentException("Usuario demo desconocido");
        active = userId;
    }
}
