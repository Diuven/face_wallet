package com.example.docker_demo.transaction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Table(name = "transaction", uniqueConstraints = {@UniqueConstraint(columnNames = {"to_address", "nonce"})})
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_hash", unique = true)
    private String transactionHash;
    @Column(name = "status", nullable = false)
    private String status;  // TODO make this enum
    @Column(name = "block_confirmation", nullable = false)
    private int blockConfirmation;  // TODO set range for this
    @Column(name = "from_address", nullable = false)
    private String fromAddress;
    @Column(name = "to_address", nullable = false)
    private String toAddress;
    @Column(name = "amount", nullable = false, columnDefinition = "DECIMAL(64, 20)")
    private BigDecimal amount;
    @Column(name = "nonce", nullable = false)
    private Long nonce;
    @Column(name = "block_hash")
    private String blockHash;
    @Column(name = "block_number")
    private BigInteger blockNumber;

    protected TransactionEntity() {
    }

    public TransactionEntity(String transactionHash,
                             String status,
                             int blockConfirmation,
                             String fromAddress,
                             String toAddress,
                             BigDecimal amount,
                             Long nonce,
                             String blockHash,
                             BigInteger blockNumber) {
        // TODO get gas data?
        this.transactionHash = transactionHash;
        this.status = status;
        this.blockConfirmation = blockConfirmation;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.nonce = nonce;
        this.blockHash = blockHash;
        this.blockNumber = blockNumber;
    }

    public TransactionEntity(String fromAddress, String toAddress, BigDecimal amount, Long nonce) {
        this("", "PENDING", 0, fromAddress, toAddress, amount, nonce, "", BigInteger.ZERO);
    }


    @Override
    public String toString() {
        return String.format(
                "TransactionEntity[\n" +
                        "id=%d, transactionHash='%s', state='%s', blockConfirmation='%s', fromAddress='%s', toAddress='%s'," +
                        "amount='%s', nonce='%d', blockHash='%s' blockNumber='%s'\n]",
                id, transactionHash, status, blockConfirmation, fromAddress, toAddress,
                amount, nonce, blockHash, blockNumber);
    }

    public Long getId() {
        return id;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getStatus() {
        return status;
    }

    public int getBlockConfirmation() {
        return blockConfirmation;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getNonce() {
        return nonce;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public void setBlockConfirmation(int blockConfirmation) {
        this.blockConfirmation = blockConfirmation;
    }

    public void setBlockNumber(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }
}