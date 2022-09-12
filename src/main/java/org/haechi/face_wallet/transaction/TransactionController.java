package org.haechi.face_wallet.transaction;

import org.haechi.face_wallet.wallet.WalletService;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
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

        TransactionEntity transactionEntity = service.sendTransactionView(credentials, fromAddress, toAddress, amount);
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
            @Param("size") Integer size) {
        walletService.fetchWalletEntityFromAddress(address); // to validate address
        List<TransactionEntity> resultList = service.listTransactionView(address, starting_after, ending_before, size);
        return ResponseEntity.ok(resultList);
    }
}
