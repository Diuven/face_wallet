package com.example.docker_demo.transaction;

import java.math.BigInteger;

public class TransactionInfoResponse {
    // https://ethereum.org/en/developers/docs/apis/json-rpc/#eth_gettransactionbyhash
    private final String fromAddress;
    private final String toAddress;
    private final BigInteger transferredValue;
    private final BigInteger gasPrice;
    private final String transactionHash;
    private final String nonce;
    private final String blockHash;
    private final BigInteger blockNumber;
    // Added
    private final String status;  // TODO make this enum
    private final String etherScanUrl;

    public TransactionInfoResponse(String fromAddress,
                                   String toAddress,
                                   BigInteger transferredValue,
                                   BigInteger gasPrice,
                                   String transactionHash,
                                   String nonce,
                                   String blockHash,
                                   BigInteger blockNumber,
                                   String status,
                                   String etherScanUrl) {
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.transferredValue = transferredValue;
        this.gasPrice = gasPrice;
        this.transactionHash = transactionHash;
        this.nonce = nonce;
        this.blockHash = blockHash;
        this.blockNumber = blockNumber;
        this.status = status;
        this.etherScanUrl = etherScanUrl;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public BigInteger getTransferredValue() {
        return transferredValue;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getNonce() {
        return nonce;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getEtherScanUrl() {
        return etherScanUrl;
    }

    @Override
    public String toString(){
        return "ASD";
    }
}
