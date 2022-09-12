package com.example.docker_demo.transaction;

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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class.getName());
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(21000);
    public static final int CONFIRMATION_LIMIT = 12;
    private final TransactionRepository transactionRepository;
    private final Validator validator;

    private final Web3j web3j = Web3j.build(new HttpService(
            "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

    public TransactionService(TransactionRepository transactionRepository, Validator validator) {
        this.transactionRepository = transactionRepository;
        this.validator = validator;
    }

    // Web3 methods
    public Credentials generateCredentialsFromPrivateKeyHex(String privateKeyHex) {
        BigInteger privateKey = toBigInteger(privateKeyHex, "private key");
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        return Credentials.create(ecKeyPair);
    }

    public BigInteger fetchCurrentGasPrice() {
        EthGasPrice ethGasPrice;
        try {
            ethGasPrice = web3j.ethGasPrice().send();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while fetching gas from the node. Is the node down?"
            );
        }
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
        EthSendTransaction ethSendTransaction = null;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(transactionHexValue).send();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while sending transaction to the node. Is the node down?"
            );
        }
        return ethSendTransaction.getTransactionHash();
    }

    public Transaction fetchTransactionInfoFromNode(String transactionHash) {
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while fetching transaction from the node. Is the node down?"
            );
        }
        return ethTransaction.getTransaction().orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "transaction")
        );
    }

    public TransactionReceipt waitForTransactionReceipt(String transactionHash) {
        TransactionReceipt receipt = null;
        for (int iter = 0; iter < 5; iter++) {
            EthGetTransactionReceipt ethGetTransactionReceiptResp = null;
            try {
                ethGetTransactionReceiptResp = web3j.ethGetTransactionReceipt(transactionHash).send();
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error while fetching transaction receipt from the node. Is the node down?"
                );
            }
            Optional<TransactionReceipt> optionalReceipt = ethGetTransactionReceiptResp.getTransactionReceipt();
            if (optionalReceipt.isPresent()) {
                receipt = optionalReceipt.get();
                break;
            }

            // Wait 3 seconds
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                logger.debug("Interrupted");
                throw new RuntimeException(e);
            }
        }

        if (receipt == null) {
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
        }
        if (!receipt.getStatus().equals("0x1")){
            throw new ResponseStatusException (HttpStatus.BAD_REQUEST, String.format("Transaction failed in the node %s", transactionHash));
        }
        return receipt;
    }

    public TransactionEntity waitAndUpdateBlockConfirmation(TransactionEntity transactionEntity, BigInteger transactionBlockNumber) {
        BigInteger lastBlockNumber = transactionBlockNumber;
        for (int iter = 0; iter < 10; iter++) {
            EthBlockNumber ethBlockNumber = null;
            try {
                ethBlockNumber = web3j.ethBlockNumber().send();
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error while fetching block number from the node. Is the node down?"
                );
            }
            BigInteger currentBlockNumber = ethBlockNumber.getBlockNumber();
            if (currentBlockNumber.compareTo(lastBlockNumber) > 0) {
                // New block mined
                BigInteger blockConfirmation = currentBlockNumber.subtract(transactionBlockNumber);
                if (blockConfirmation.intValue() > CONFIRMATION_LIMIT) {
                    transactionEntity.setBlockConfirmation(CONFIRMATION_LIMIT);
                    transactionEntity.setStatus("CONFIRMED");
                    return transactionRepository.save(transactionEntity);
                }
                else {
                    transactionEntity.setBlockConfirmation(blockConfirmation.intValue());
                    transactionEntity = transactionRepository.save(transactionEntity);
                    lastBlockNumber = currentBlockNumber;
                    iter = 0; // Reset retry count
                }
            }
            // Wait 5 seconds
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.debug("Interrupted");
                throw new RuntimeException(e);
            }
        }
        // Unsuccessful
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Timeout during waiting for block confirmation. Is node down?"
        );
    }

    // DB methods
    public TransactionEntity fetchTransactionInfo(String transactionHash) {
        return transactionRepository.findByTransactionHash(transactionHash).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "transaction")
        );
    }

    public TransactionEntity createTransactionEntity(String fromAddress, String toAddress, BigDecimal amount, Long nonce) {
        TransactionEntity transactionEntity = new TransactionEntity(
                fromAddress, toAddress, amount, nonce
        );
        return transactionRepository.save(transactionEntity);
    }

    public TransactionEntity updateMinedTransactionEntity(TransactionEntity transactionEntity, TransactionReceipt receipt) {
        DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(receipt.getBlockNumber());
        Date minedDate;
        try {
            EthBlock ethBlock = web3j.ethGetBlockByNumber(blockParameter, true).send();
            minedDate = new Date(ethBlock.getBlock().getTimestamp().longValue());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while fetching block info from the node. Is the node down?"
            );
        }
        transactionEntity.setStatus("MINED");
        transactionEntity.setTransactionHash(receipt.getTransactionHash());
        transactionEntity.setBlockHash(receipt.getBlockHash());
        transactionEntity.setBlockNumber(receipt.getBlockNumber());
        transactionEntity.setTs(minedDate);
        return transactionRepository.save(transactionEntity);
    }

    public List<TransactionEntity> fetchTransactionList(String address, Date startDate, Date endDate, int size) {
        return transactionRepository.findFirstKByAddress(address, startDate, endDate, size);
    }

    // DTO methods
    public BigInteger toBigInteger(String value){
        return toBigInteger(value, "hex value");
    }
    public BigInteger toBigInteger(String value, String name) {
        // TODO make this common util
        try {
            return new BigInteger(value, 16);
        }
        catch (NumberFormatException e){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Error while parsing " + name
            );
        }
    }

    public BigDecimal toBigDecimal(String value) {
        return toBigDecimal(value, "decimal");
    }

    public BigDecimal toBigDecimal(String value, String name) {
        // TODO make this common util
        try {
            return new BigDecimal(value);
        }
        catch (NumberFormatException e){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Error while parsing " + name
            );
        }
    }

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
}
