package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.model.LedgerEntry;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WalletRulesTest {
    @Test
    void skillCoinsRejectNegativeValuesInsufficientFundsAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(1).minus(new SkillCoins(2)));
        assertThrows(ArithmeticException.class,
                () -> new SkillCoins(Integer.MAX_VALUE).plus(new SkillCoins(1)));
        assertEquals(0, new SkillCoins(2).minus(new SkillCoins(2)).amount());
    }

    @Test
    void walletCanOpenEmptyOrWithAnInitialCredit() {
        Wallet empty = Wallet.open(" owner ", 0);
        assertEquals("owner", empty.userId());
        assertEquals(0, empty.balance().amount());
        assertTrue(empty.movements().isEmpty());

        Wallet initialized = Wallet.open("owner", 50);
        assertEquals(50, initialized.balance().amount());
        assertEquals(1, initialized.movements().size());
        assertEquals(50, initialized.movements().get(0).amount());
        assertEquals(50, initialized.movements().get(0).resultingBalance());

        assertThrows(IllegalArgumentException.class, () -> Wallet.open("owner", -1));
        assertThrows(IllegalArgumentException.class, () -> Wallet.open(" ", 10));
    }

    @Test
    void validCreditsAndDebitsUpdateBalanceAndAppendConsistentMovements() {
        Wallet wallet = Wallet.open("owner", 50);

        wallet.credit(15, "  Bonificación  ");
        wallet.debit(20, "  Sesión de mentoría  ");

        assertEquals(45, wallet.balance().amount());
        assertEquals(List.of(50, 15, -20), wallet.movements().stream().map(LedgerEntry::amount).toList());
        assertEquals(List.of(50, 65, 45),
                wallet.movements().stream().map(LedgerEntry::resultingBalance).toList());
        assertEquals("Bonificación", wallet.movements().get(1).concept());
        assertEquals("Sesión de mentoría", wallet.movements().get(2).concept());
    }

    @Test
    void invalidMovementAmountsAndConceptsDoNotChangeWallet() {
        Wallet wallet = Wallet.open("owner", 50);
        List<LedgerEntry> originalMovements = wallet.movements();

        assertThrows(IllegalArgumentException.class, () -> wallet.credit(0, "Crédito"));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(-1, "Crédito"));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(0, "Débito"));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(-1, "Débito"));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(1, " "));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(1, " "));

        assertEquals(50, wallet.balance().amount());
        assertEquals(originalMovements, wallet.movements());
    }

    @Test
    void insufficientFundsAndCreditOverflowLeaveBalanceAndLedgerUntouched() {
        Wallet wallet = Wallet.open("owner", 50);
        List<LedgerEntry> originalMovements = wallet.movements();

        assertThrows(IllegalArgumentException.class, () -> wallet.debit(51, "Débito"));

        assertEquals(50, wallet.balance().amount());
        assertEquals(originalMovements, wallet.movements());

        Wallet atMaximum = Wallet.open("another-owner", Integer.MAX_VALUE);
        List<LedgerEntry> maximumMovements = atMaximum.movements();
        assertThrows(ArithmeticException.class, () -> atMaximum.credit(1, "Desbordamiento"));
        assertEquals(Integer.MAX_VALUE, atMaximum.balance().amount());
        assertEquals(maximumMovements, atMaximum.movements());
    }

    @Test
    void reconstitutesWalletFromAConsistentChronologicalLedger() {
        LocalDateTime firstMovementAt = LocalDateTime.parse("2026-10-01T10:00:00");
        List<LedgerEntry> movements = List.of(
                movement("credit-1", 25, 25, firstMovementAt),
                movement("debit-1", -7, 18, firstMovementAt.plusHours(1)));

        Wallet wallet = new Wallet(" owner ", new SkillCoins(18), movements);

        assertEquals("owner", wallet.userId());
        assertEquals(18, wallet.balance().amount());
        assertEquals(movements, wallet.movements());
        assertThrows(UnsupportedOperationException.class, () -> wallet.movements().clear());
    }

    @Test
    void rejectsInconsistentDuplicateAndOutOfOrderLedgerDuringReconstitution() {
        LocalDateTime earlier = LocalDateTime.parse("2026-10-01T10:00:00");
        LocalDateTime later = earlier.plusHours(1);

        assertThrows(IllegalArgumentException.class, () -> new Wallet("owner", new SkillCoins(4),
                List.of(movement("credit-1", 5, 5, earlier))));
        assertThrows(IllegalArgumentException.class, () -> new Wallet("owner", new SkillCoins(4),
                List.of(movement("credit-1", 5, 4, earlier))));
        assertThrows(IllegalArgumentException.class, () -> new Wallet("owner", SkillCoins.zero(),
                List.of(movement("debit-1", -1, 0, earlier))));
        assertThrows(IllegalArgumentException.class, () -> new Wallet("owner", new SkillCoins(7), List.of(
                movement("same-id", 5, 5, earlier), movement("same-id", 2, 7, later))));
        assertThrows(IllegalArgumentException.class, () -> new Wallet("owner", new SkillCoins(7), List.of(
                movement("later-first", 5, 5, later), movement("earlier-second", 2, 7, earlier))));
    }

    private static LedgerEntry movement(String id, int amount, int resultingBalance, LocalDateTime occurredAt) {
        return new LedgerEntry(id, "Movimiento", amount, resultingBalance, occurredAt);
    }
}
