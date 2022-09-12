package com.example.docker_demo.transaction;

import com.example.docker_demo.wallet.WalletEntity;
import com.example.docker_demo.wallet.WalletService;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;

@RestController
@RequestMapping("transaction")
public class TransactionController {
    private final TransactionService service;
    private final WalletService walletService;

    public TransactionController(TransactionService service, WalletService walletService) {
        this.service = service;
        this.walletService = walletService;
    }

    @PostMapping("/send")
    public ResponseEntity<TransactionEntity> sendTransaction(@RequestBody TransactionSendRequest request) {
        service.validateTransactionSendRequest(request);
        Credentials credentials = service.generateCredentialsFromPrivateKeyHex(request.getPrivateKeyHex());
        BigDecimal amount = service.toBigDecimal(request.getAmount(), "amount");
        String toAddress = request.getToAddress().toLowerCase();
        String fromAddress = credentials.getAddress().toLowerCase();

        // Get and update wallet
        WalletEntity fromWalletEntity = walletService.fetchWalletEntityFromAddress(fromAddress);
        walletService.updateWalletInfo(fromWalletEntity);
        if (fromWalletEntity.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds balance");
        }
        BigInteger nonce = BigInteger.valueOf(fromWalletEntity.getNonce());

        // TODO set gas priority from request?
        BigInteger gasPrice = service.fetchCurrentGasPrice();

        // Create transaction
        String transactionHexValue = service.createRawTransactionInHex(
                nonce, gasPrice, toAddress, amount, credentials
        );
        // Write in db
        TransactionEntity transactionEntity = service.createTransactionEntity(
                fromAddress, toAddress, amount, nonce.longValue()
        );
        // Send transaction
        String transactionHash = service.sendRawTransaction(transactionHexValue);

        // Wait for transaction to be mined
        TransactionReceipt receipt = service.waitForTransactionReceipt(transactionHash);
        // Update db
        transactionEntity = service.updateMinedTransactionEntity(transactionEntity, receipt);
        fromWalletEntity = walletService.increaseWalletEntityNonce(fromWalletEntity);

        // Wait for confirmation
        service.waitForBlockConfirmation(transactionHash, receipt.getBlockNumber());
        // Update DB
        transactionEntity = service.updateConfirmedTransactionEntity(transactionEntity);
        fromWalletEntity = walletService.setWalletEntityBalance(
                fromWalletEntity, fromWalletEntity.getBalance().subtract(amount)
        );
        walletService.tryIncreaseWalletEntityBalance(toAddress, amount);

        return ResponseEntity.ok(transactionEntity);
    }

    // TODO make async send


    @GetMapping("/info")
    public ResponseEntity<Transaction> transactionInfo(@Param("transactionHash") String transactionHash) {
        Transaction transaction = service.fetchTransactionInfo(transactionHash);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/list")
    public String list(@Param("address") String address, @Param("privateKeyHex") String privateKeyHex) throws Exception {
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials credentials = Credentials.create(ecKeyPair);

        // TODO check credentials
        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        return "Not yet implemeneted";
    }
}
