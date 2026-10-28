
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_ledger")
public class WalletLedgerEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private WalletEntity wallet;

    @Column(name = "concept", length = 250, nullable = false)
    private String concept;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "resulting_balance", nullable = false)
    private int resultingBalance;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public WalletLedgerEntity() {
    }

    public String getId() {
        return id;
    }

    public WalletEntity getWallet() {
        return wallet;
    }

    public String getConcept() {
        return concept;
    }

    public int getAmount() {
        return amount;
    }

    public int getResultingBalance() {
        return resultingBalance;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setWallet(WalletEntity wallet) {
        this.wallet = wallet;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setResultingBalance(int resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
