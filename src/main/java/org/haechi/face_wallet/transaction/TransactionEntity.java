package org.haechi.face_wallet.transaction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(
        name = "transaction",
        indexes = {@Index(columnList = "to_address, ts"), @Index(columnList = "from_address, ts")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"from_address", "nonce"})}
)
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
    @Column(name = "ts")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ts;

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
                             BigInteger blockNumber,
                             Date ts) {
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
        this.ts = ts;
    }

    public TransactionEntity(String fromAddress, String toAddress, BigDecimal amount, Long nonce) {
        this(null, "PENDING", 0, fromAddress, toAddress, amount, nonce, "", BigInteger.ZERO, new Date());
    }


    @Override
    public String toString() {
        return String.format(
                "TransactionEntity[\n" +
                        "id=%d, transactionHash='%s', state='%s', blockConfirmation='%s', fromAddress='%s', toAddress='%s'," +
                        "amount='%s', nonce='%d', blockHash='%s' blockNumber='%s', ts='%s'\n]",
                id, transactionHash, status, blockConfirmation, fromAddress, toAddress,
                amount, nonce, blockHash, blockNumber, ts);
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

    public Date getTs() {
        return ts;
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

    public void setTs(Date ts) {
        this.ts = ts;
    }
}
