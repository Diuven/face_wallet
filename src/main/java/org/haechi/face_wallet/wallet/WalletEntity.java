package org.haechi.face_wallet.wallet;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "wallet")
public class WalletEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "address", nullable = false, unique = true)
    private String address;
    @Column(name = "nonce", nullable = false)
    private Long nonce;
    @Column(name = "balance", nullable = false, columnDefinition = "DECIMAL(64, 20)")
    private BigDecimal balance;
    @Column(name = "balance_on_hold", nullable = false, columnDefinition = "DECIMAL(64, 20)")
    private BigDecimal balanceOnHold;

    protected WalletEntity() {}

    public WalletEntity(String address) {
        this(address, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public WalletEntity(String address, Long nonce, BigDecimal balance, BigDecimal balanceOnHold) {
        this.address = address;
        this.nonce = nonce;
        this.balance = balance;
        this.balanceOnHold = balanceOnHold;
    }

    @Override
    public String toString() {
        return String.format(
                "WalletEntity[id=%d, address='%s', nonce='%d', balance='%s' balanceOnHold='%s']",
                id, address, nonce, balance, balanceOnHold);
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public Long getNonce() {
        return nonce;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getBalanceOnHold() {
        return balanceOnHold;
    }

    public void setNonce(Long nonce) {
        this.nonce = nonce;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setBalanceOnHold(BigDecimal balanceOnHold) {
        this.balanceOnHold = balanceOnHold;
    }
}
