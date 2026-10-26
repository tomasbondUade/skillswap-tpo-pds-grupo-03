package ar.edu.uade.skillswap.domain.model;

import java.time.LocalDateTime;

public record LedgerEntry(String id, String concept, int amount, int resultingBalance, LocalDateTime occurredAt) {
    public LedgerEntry {
        if (id == null || id.isBlank() || concept == null || concept.isBlank()) throw new IllegalArgumentException("Movimiento incompleto");
        if (amount == 0 || resultingBalance < 0 || occurredAt == null) throw new IllegalArgumentException("Movimiento inválido");
        id = id.trim();
        concept = concept.trim();
    }
}
