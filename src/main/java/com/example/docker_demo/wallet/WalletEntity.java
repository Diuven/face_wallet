package com.example.docker_demo.wallet;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    public void setNonce(Long nonce) {
        this.nonce = nonce;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletEntity that = (WalletEntity) o;
        return address.equals(that.address) && nonce.equals(that.nonce) && balance.compareTo(that.balance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, nonce, balance);
    }
}
