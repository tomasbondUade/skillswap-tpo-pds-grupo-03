package ar.edu.uade.skillswap.infrastructure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoCurrentUserProviderTest {
    @Test
    void preservesTheInjectedIdentityUntilTheUserExplicitlySwitches() {
        String[] original = {"user-mateo"};
        var identity = new DemoCurrentUserProvider(() -> original[0]);
        assertEquals("user-mateo", identity.currentUserId());
        original[0] = "mentor-ana";
        assertEquals("mentor-ana", identity.currentUserId());
        identity.switchTo("mentor-lu");
        assertEquals("mentor-lu", identity.currentUserId());
    }

    @Test
    void rejectsUnknownOrMissingDemoUsersWithoutChangingTheSession() {
        var identity = new DemoCurrentUserProvider();
        identity.switchTo("mentor-ana");
        assertThrows(IllegalArgumentException.class, () -> identity.switchTo("unknown"));
        assertThrows(IllegalArgumentException.class, () -> identity.switchTo(null));
        assertEquals("mentor-ana", identity.currentUserId());
    }
}
