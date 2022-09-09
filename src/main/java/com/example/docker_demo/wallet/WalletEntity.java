package com.example.docker_demo.wallet;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Table(name = "wallet")
public class WalletEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "address", nullable = false, unique = true)
    private String address;
    @Column(name = "nonce", nullable = false)
    private Long nonce;
    @Column(name = "balance", nullable = false, columnDefinition = "DECIMAL(64, 20)")
    private BigDecimal balance;

    protected WalletEntity() {}

    public WalletEntity(String address) {
        this(address, 0L, BigDecimal.ZERO);
    }

    public WalletEntity(String address, Long nonce, BigDecimal balance) {
        this.address = address;
        this.nonce = nonce;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return String.format(
                "WalletEntity[id=%d, address='%s', nonce='%d', balance='%s']",
                id, address, nonce, balance);
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

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
