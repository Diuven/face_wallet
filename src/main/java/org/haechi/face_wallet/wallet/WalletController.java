package org.haechi.face_wallet.wallet;

import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;

@RestController
@RequestMapping("wallet")
public class WalletController {
    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public WalletCreateResponse createWallet(@RequestBody WalletCreateRequest request) {
        service.validateCreateRequest(request);
        String password = request.getPassword();
        String mnemonic = service.generateRandomMnemonic();
        Credentials credentials = service.generateCredentialsFromMnemonicAndPassword(mnemonic, password);
        WalletEntity walletEntity = service.createWalletEntityFromCredentials(credentials);

        // Return address, mnemonic, and private key to user
        return service.createWalletCreateResponse(walletEntity, mnemonic, credentials);
    }

    @GetMapping("/info")
    public ResponseEntity<WalletEntity> getWalletInfo(@Param("address") String address) {
        address = address.toLowerCase();
        WalletEntity walletEntity = service.fetchWalletEntityFromAddress(address);
        return ResponseEntity.ok(walletEntity);
    }

    @GetMapping("/sync")
    public ResponseEntity<WalletEntity> getWalletSync(@Param("address") String address) {
        // Sync db with web3 node
        address = address.toLowerCase();
        WalletEntity walletEntity = service.fetchWalletEntityFromAddressFromNode(address);
        return ResponseEntity.ok(walletEntity);
    }
}
