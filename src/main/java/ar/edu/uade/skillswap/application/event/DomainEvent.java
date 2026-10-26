package ar.edu.uade.skillswap.application.event;

import java.time.LocalDateTime;
import java.util.Map;

/** Framework-free integration event published after an application transaction succeeds. */
public record DomainEvent(String name, String aggregateType, String aggregateId,
                          String actorUserId, LocalDateTime occurredAt,
                          Map<String, String> attributes) {
    public DomainEvent {
        if (name == null || name.isBlank() || aggregateType == null || aggregateType.isBlank()
                || aggregateId == null || aggregateId.isBlank() || occurredAt == null) {
            throw new IllegalArgumentException("El evento está incompleto");
        }
        name = name.trim();
        aggregateType = aggregateType.trim();
        aggregateId = aggregateId.trim();
        actorUserId = actorUserId == null ? null : actorUserId.trim();
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
    }
}
