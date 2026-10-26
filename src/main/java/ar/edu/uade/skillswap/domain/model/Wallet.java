package ar.edu.uade.skillswap.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Wallet aggregate: saldo y ledger cambian juntos y no admiten montos inválidos. */
public final class Wallet {
    private final String userId;
    private SkillCoins balance;
    private final List<LedgerEntry> movements = new ArrayList<>();

    /** Constructor de reconstitución; valida que el ledger corresponda con el saldo guardado. */
    public Wallet(String userId, SkillCoins balance, Collection<LedgerEntry> movements) {
        this.userId = required(userId, "Falta titular de wallet");
        this.balance = Objects.requireNonNull(balance, "Falta saldo");
        if (movements != null) {
            for (LedgerEntry movement : movements) this.movements.add(Objects.requireNonNull(movement));
        }
        validateLedger();
    }

    public static Wallet open(String userId, int initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        Wallet wallet = new Wallet(userId, SkillCoins.zero(), List.of());
        if (initialBalance > 0) wallet.credit(initialBalance, "Saldo inicial");
        return wallet;
    }

    public void credit(int amount, String concept) {
        validateMovement(amount, concept);
        SkillCoins updatedBalance = balance.plus(new SkillCoins(amount));
        appendMovement(amount, concept, updatedBalance);
    }

    public void debit(int amount, String concept) {
        validateMovement(amount, concept);
        SkillCoins updatedBalance = balance.minus(new SkillCoins(amount));
        appendMovement(-amount, concept, updatedBalance);
    }

    private void appendMovement(int signedAmount, String concept, SkillCoins updatedBalance) {
        LedgerEntry entry = new LedgerEntry(java.util.UUID.randomUUID().toString(), concept.trim(),
                signedAmount, updatedBalance.amount(), LocalDateTime.now());
        movements.add(entry);
        balance = updatedBalance;
    }

    private static void validateMovement(int amount, String concept) {
        if (amount <= 0) throw new IllegalArgumentException("El movimiento debe ser positivo");
        required(concept, "El concepto del movimiento es obligatorio");
    }

    private void validateLedger() {
        int expectedBalance = 0;
        Set<String> movementIds = new HashSet<>();
        LocalDateTime previousTimestamp = null;
        for (LedgerEntry movement : movements) {
            if (!movementIds.add(movement.id())) throw new IllegalArgumentException("El ledger no puede repetir movimientos");
            if (previousTimestamp != null && movement.occurredAt().isBefore(previousTimestamp)) {
                throw new IllegalArgumentException("Los movimientos deben estar ordenados cronológicamente");
            }
            expectedBalance = Math.addExact(expectedBalance, movement.amount());
            if (expectedBalance < 0 || movement.resultingBalance() != expectedBalance) {
                throw new IllegalArgumentException("El ledger contiene un saldo inconsistente");
            }
            previousTimestamp = movement.occurredAt();
        }
        if (expectedBalance != balance.amount()) throw new IllegalArgumentException("El saldo no coincide con los movimientos del ledger");
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public String userId() { return userId; }
    public SkillCoins balance() { return balance; }
    public List<LedgerEntry> movements() { return List.copyOf(movements); }
}
