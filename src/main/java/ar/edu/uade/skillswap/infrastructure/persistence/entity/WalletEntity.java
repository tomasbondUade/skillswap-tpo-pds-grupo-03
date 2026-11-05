
package ar.edu.uade.skillswap.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @Column(name = "balance", nullable = false)
    private int balance;

    @OneToMany(
            mappedBy = "wallet",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WalletLedgerEntity> movements = new ArrayList<>();

    public WalletEntity() {
    }

    public String getUserId() {
        return userId;
    }

    public int getBalance() {
        return balance;
    }

    public List<WalletLedgerEntity> getMovements() {
        return movements;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void addMovement(WalletLedgerEntity movement) {
        movements.add(movement);
        movement.setWallet(this);
    }
}
