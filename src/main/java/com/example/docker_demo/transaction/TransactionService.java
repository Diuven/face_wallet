package com.example.docker_demo.transaction;

import com.example.docker_demo.utils;
import com.example.docker_demo.wallet.WalletEntity;
import com.example.docker_demo.wallet.WalletService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Service
public class TransactionService extends utils {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class.getName());
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(21000);
    public static final int CONFIRMATION_LIMIT = 12;
    private final WalletService walletService;
    private final TransactionRepository repository;
    private final Validator validator;

    private final Web3j web3j = Web3j.build(new HttpService(
            "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

    public TransactionService(WalletService walletService, TransactionRepository repository, Validator validator) {
        this.walletService = walletService;
        this.repository = repository;
        this.validator = validator;
    }

    // View methods
    public TransactionEntity sendTransactionView(Credentials credentials, String fromAddress, String toAddress, BigDecimal amount) {
        // Check wallet
        WalletEntity fromWalletEntity = walletService.fetchWalletEntityFromAddress(fromAddress);
        WalletEntity toWalletEntity = walletService.fetchWalletEntityFromAddress(toAddress);
        if (!walletService.isSufficientBalance(fromWalletEntity, amount)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds available balance");
        }
        BigInteger nonce = BigInteger.valueOf(fromWalletEntity.getNonce());

        // Write db
        TransactionEntity transactionEntity = createTransactionEntity(fromAddress, toAddress, amount, nonce.longValue());
        fromWalletEntity = walletService.addPendingTransaction(fromWalletEntity, amount);

        // TODO set gas priority from request?
        BigInteger gasPrice = fetchCurrentGasPrice();
        // Create and send transaction
        String transactionHexValue = createRawTransactionInHex(nonce, gasPrice, toAddress, amount, credentials);
        String transactionHash = sendRawTransaction(transactionHexValue);

        // Wait for transaction to be mined
        TransactionReceipt receipt = waitForTransactionReceipt(transactionHash);
        // Update db
        transactionEntity = updateMinedTransactionEntity(transactionEntity, receipt);

        // Wait for confirmation and update
        transactionEntity = waitAndUpdateBlockConfirmation(transactionEntity, receipt.getBlockNumber());
        Iterable<WalletEntity> wallets = walletService.updateConfirmedTransaction(fromWalletEntity, toWalletEntity, amount);

        return transactionEntity;
    }

    public List<TransactionEntity> listTransactionView(String address, String starting_after, String ending_before, Integer size) {
        if (size == null){
            size = 10;
        }
        List<TransactionEntity> txList = validateTransactionListRequest(address, starting_after, ending_before, size);

        Date startDate = null, endDate = null;
        if (txList.get(0) != null) {
            startDate = txList.get(0).getTs();
        }
        if (txList.get(1) != null) {
            endDate = txList.get(1).getTs();
        }

        List<TransactionEntity> resultList = repository.findFirstKByAddress(address, startDate, endDate, size);
        Collections.reverse(resultList); // most recent tx goes to the front
        return resultList;
    }

    // Web3 methods
    public Credentials generateCredentialsFromPrivateKeyHex(String privateKeyHex) {
        BigInteger privateKey = toBigInteger(privateKeyHex, "private key");
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        return Credentials.create(ecKeyPair);
    }

    public BigInteger fetchCurrentGasPrice() {
        EthGasPrice ethGasPrice = send(web3j.ethGasPrice(), "fetching gas from");
        return ethGasPrice.getGasPrice();
    }

    public String createRawTransactionInHex(BigInteger nonce,
                                            BigInteger gasPrice,
                                            String toAddress,
                                            BigDecimal valueInEth,
                                            Credentials credentials) {
        BigInteger valueInWei = Convert.toWei(valueInEth, Convert.Unit.ETHER).toBigInteger();
        RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(
                nonce, gasPrice, GAS_LIMIT, toAddress, valueInWei);

        // Sign the transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        return Numeric.toHexString(signedMessage);
    }

    public String sendRawTransaction(String transactionHexValue) {
        EthSendTransaction ethSendTransaction = send(
                web3j.ethSendRawTransaction(transactionHexValue),"sending transaction to"
        );
        return ethSendTransaction.getTransactionHash();
    }

    public TransactionReceipt waitForTransactionReceipt(String transactionHash) {
        TransactionReceipt receipt = null;
        for (int iter = 0; iter < 10; iter++) {
            EthGetTransactionReceipt ethGetTransactionReceiptResp = send(
                    web3j.ethGetTransactionReceipt(transactionHash),"fetching transaction receipt from"
            );
            Optional<TransactionReceipt> optionalReceipt = ethGetTransactionReceiptResp.getTransactionReceipt();
            if (optionalReceipt.isPresent()) {
                receipt = optionalReceipt.get();
                break;
            }

            // Wait 5 seconds
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.debug("Interrupted");
                throw new RuntimeException(e);
            }
        }

        if (receipt == null) {
            throw new ResponseStatusException(
                    HttpStatus.REQUEST_TIMEOUT,
                    "Timeout while waiting for receipt from the node. Is transaction invalid?"
            );
        }
        if (!receipt.getStatus().equals("0x1")){
            throw new ResponseStatusException (HttpStatus.BAD_REQUEST, String.format("Transaction failed in the node %s", transactionHash));
        }
        return receipt;
    }

    public TransactionEntity waitAndUpdateBlockConfirmation(TransactionEntity transactionEntity, BigInteger transactionBlockNumber) {
        BigInteger lastBlockNumber = transactionBlockNumber;
        for (int iter = 0; iter < 10; iter++) {
            EthBlockNumber ethBlockNumber = send(
                    web3j.ethBlockNumber(),"fetching block number from"
            );
            BigInteger currentBlockNumber = ethBlockNumber.getBlockNumber();
            if (currentBlockNumber.compareTo(lastBlockNumber) > 0) {
                // New block mined
                BigInteger blockConfirmation = currentBlockNumber.subtract(transactionBlockNumber);
                if (blockConfirmation.intValue() >= CONFIRMATION_LIMIT) {
                    transactionEntity.setBlockConfirmation(CONFIRMATION_LIMIT);
                    transactionEntity.setStatus("CONFIRMED");
                    return repository.save(transactionEntity);
                }
                else {
                    transactionEntity.setBlockConfirmation(blockConfirmation.intValue());
                    transactionEntity = repository.save(transactionEntity);
                    lastBlockNumber = currentBlockNumber;
                    iter = 0; // Reset retry count
                }
            }
            // Wait 5 seconds
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.debug("Interrupted");
                throw new RuntimeException(e);
            }
        }
        // Unsuccessful
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Timeout during waiting for block confirmation. Is the node down?"
        );
    }

    // DB methods
    public TransactionEntity fetchTransactionInfo(String transactionHash) {
        return repository.findByTransactionHash(transactionHash).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "transaction")
        );
    }

    public TransactionEntity createTransactionEntity(String fromAddress, String toAddress, BigDecimal amount, Long nonce) {
        try {
            TransactionEntity transactionEntity = new TransactionEntity(
                    fromAddress, toAddress, amount, nonce
            );
            return repository.save(transactionEntity);
        }
        catch (RuntimeException e) {
            logger.warn(e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate tx with same key exists.");
        }
    }

    public TransactionEntity updateMinedTransactionEntity(TransactionEntity transactionEntity, TransactionReceipt receipt) {
        DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(receipt.getBlockNumber());
        EthBlock ethBlock = send(
                web3j.ethGetBlockByNumber(blockParameter, true),"fetching block info from"
        );
        Date minedDate = new Date(ethBlock.getBlock().getTimestamp().longValue() * 1000);

        transactionEntity.setStatus("MINED");
        transactionEntity.setTransactionHash(receipt.getTransactionHash());
        transactionEntity.setBlockHash(receipt.getBlockHash());
        transactionEntity.setBlockNumber(receipt.getBlockNumber());
        transactionEntity.setTs(minedDate);
        return repository.save(transactionEntity);
    }

    // DTO methods
    public boolean validateTransactionSendRequest(TransactionSendRequest request) {
        Set<ConstraintViolation<TransactionSendRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<TransactionSendRequest> constraintViolation : violations) {
                sb.append(constraintViolation.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, sb.toString());
        }

        return true;
    }

    public List<TransactionEntity> validateTransactionListRequest(String address, String starting_after, String ending_before, int size) {
        if (size <= 0 || size > 100) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "size should be in 1 to 100");
        }
        TransactionEntity startTx = null, endTx = null;
        if (starting_after != null) {
            startTx = fetchTransactionInfo(starting_after);
            if (!startTx.getFromAddress().equals(address) && !startTx.getToAddress().equals(address)){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "starting_after is not related to given address");
            }
        }
        if (ending_before != null) {
            endTx = fetchTransactionInfo(ending_before);
            if (!endTx.getFromAddress().equals(address) && !endTx.getToAddress().equals(address)){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ending_before is not related to given address");
            }
        }
        List<TransactionEntity> resultList = new ArrayList<>();
        resultList.add(startTx);
        resultList.add(endTx);
        return resultList;
    }

    @Override
    public void logWeb3Action(String msg) {
        logger.info(msg + " the node");
    }
}
