package com.example.docker_demo.wallet;

import java.math.BigDecimal;
import java.math.BigInteger;

public class WalletBalanceResponse {
    private final String address;
    private final BigInteger balanceInWei;
    private final BigDecimal balanceInEth;

    public WalletBalanceResponse(String address, BigInteger balanceInWei, BigDecimal balanceInEth) {
        this.address = address;
        this.balanceInWei = balanceInWei;
        this.balanceInEth = balanceInEth;
    }

    public String getAddress() {
        return address;
    }

    public BigInteger getBalanceInWei() {
        return balanceInWei;
    }

    public BigDecimal getBalanceInEth() {
        return balanceInEth;
    }
}
