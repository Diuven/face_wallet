package com.example.docker_demo.transaction;

import java.math.BigDecimal;

public class TransactionSendRequest {
    private String toAddress;
    private String privateKeyHex;
    private String amount;

    public TransactionSendRequest() {}

    public TransactionSendRequest(String toAddress, String privateKeyHex, String amount) {
        this.toAddress = toAddress;
        this.privateKeyHex = privateKeyHex;
        this.amount = amount;
    }

    public String getToAddress() {
        return toAddress;
    }

    public String getPrivateKeyHex() {
        return privateKeyHex;
    }

    public String getAmount() {
        return amount;
    }

    public BigDecimal getAmountAsDecimal() {
        return new BigDecimal(this.amount);
    }
}
