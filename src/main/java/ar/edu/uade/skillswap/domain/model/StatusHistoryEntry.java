package ar.edu.uade.skillswap.domain.model;

import java.time.LocalDateTime;

/** Immutable audit record for a state change. */
public record StatusHistoryEntry(String from, String to, String actorUserId,
                                String reason, LocalDateTime occurredAt) {
    public StatusHistoryEntry {
        if (actorUserId == null || actorUserId.isBlank()) throw new IllegalArgumentException("Falta el actor del cambio");
        if (to == null || to.isBlank()) throw new IllegalArgumentException("Falta el estado destino");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Falta el motivo del cambio");
        if (occurredAt == null) throw new IllegalArgumentException("Falta la fecha del cambio");
        from = from == null ? null : from.trim();
        to = to.trim();
        actorUserId = actorUserId.trim();
        reason = reason.trim();
    }
}
