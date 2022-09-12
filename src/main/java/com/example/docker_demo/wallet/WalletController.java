package com.example.docker_demo.wallet;

import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;

import java.util.logging.Logger;

@RestController
@RequestMapping("wallet")
public class WalletController {
    private static final Logger logger = Logger.getLogger(WalletRepository.class.getName());
    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public WalletCreateResponse createWallet(@RequestBody WalletCreateRequest request) {
        String password = service.validateCreateRequest(request);
        String mnemonic = service.generateRandomMnemonicFromPassword(password);
        Credentials credentials = service.generateCredentialsFromMnemonicAndPassword(mnemonic, password);
        WalletEntity walletEntity = service.createWalletEntityFromCredentials(credentials);

        // Return address, mnemonic, and private key to user
        return service.createWalletCreateResponse(walletEntity, mnemonic, credentials);
    }

    @GetMapping("/info")
    public ResponseEntity<WalletEntity> getWalletInfo(@Param("address") String address) {
        address = address.toLowerCase();
        WalletEntity walletEntity = service.fetchWalletEntityFromAddress(address);
        walletEntity = service.updateWalletInfo(walletEntity);

        // Return balance, nonce and address to user
        return ResponseEntity.ok(walletEntity);
    }
}
