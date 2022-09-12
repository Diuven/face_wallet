package com.example.docker_demo.transaction;

import com.example.docker_demo.wallet.WalletEntity;
import com.example.docker_demo.wallet.WalletService;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

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

        // Check wallet
        WalletEntity fromWalletEntity = walletService.fetchWalletEntityFromAddress(fromAddress);
        WalletEntity toWalletEntity = walletService.fetchWalletEntityFromAddress(toAddress);
        if (!walletService.isSufficientBalance(fromWalletEntity, amount)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds available balance");
        }
        BigInteger nonce = BigInteger.valueOf(fromWalletEntity.getNonce());
        // TODO set gas priority from request?
        BigInteger gasPrice = service.fetchCurrentGasPrice();

        // Create transaction
        String transactionHexValue = service.createRawTransactionInHex(
                nonce, gasPrice, toAddress, amount, credentials
        );
        // Write db
        TransactionEntity transactionEntity = service.createTransactionEntity(
                fromAddress, toAddress, amount, nonce.longValue()
        );
        fromWalletEntity = walletService.addPendingTransaction(fromWalletEntity, amount);

        // Send transaction
        String transactionHash = service.sendRawTransaction(transactionHexValue);

        // Wait for transaction to be mined
        TransactionReceipt receipt = service.waitForTransactionReceipt(transactionHash);
        // Update db
        transactionEntity = service.updateMinedTransactionEntity(transactionEntity, receipt);

        // Wait for confirmation and update
        transactionEntity = service.waitAndUpdateBlockConfirmation(transactionEntity, receipt.getBlockNumber());
        Iterable<WalletEntity> wallets = walletService.updateConfirmedTransaction(fromWalletEntity, toWalletEntity, amount);

        return ResponseEntity.ok(transactionEntity);
    }

    // TODO make async send


    @GetMapping("/info")
    public ResponseEntity<TransactionEntity> transactionInfo(@Param("transactionHash") String transactionHash) {
        TransactionEntity transactionEntity = service.fetchTransactionInfo(transactionHash);
        return ResponseEntity.ok(transactionEntity);
    }

    @GetMapping("/list")
    public ResponseEntity<List<TransactionEntity>> transactionList(
            @NotNull @Param("address") String address,
            @Param("starting_after") String starting_after,
            @Param("ending_before") String ending_before,
            @Param("size") int size) {
        walletService.fetchWalletEntityFromAddress(address); // to validate address
        List<TransactionEntity> txList = service.validateTransactionListRequest(address, starting_after, ending_before, size);

        Date startDate = null, endDate = null;
        if (txList.get(0) != null) {
            startDate = txList.get(0).getTs();
        }
        if (txList.get(1) != null) {
            endDate = txList.get(1).getTs();
        }

        return ResponseEntity.ok(service.fetchTransactionList(address, startDate, endDate, size));
    }
}
