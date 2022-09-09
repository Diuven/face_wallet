package com.example.docker_demo.wallet;

import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.logging.Logger;

import static org.web3j.crypto.Hash.sha256;

@RestController
@RequestMapping("wallet")
public class WalletController {
    private static final Logger logger = Logger.getLogger("WalletRepository");
    private final WalletRepository repository;
    private static final SecureRandom secureRandom = new SecureRandom();

    public WalletController(WalletRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/create")
    public WalletCreateResponse createWallet(@RequestBody WalletCreateRequest request) {
        String password = request.getPassword();

        // TODO update web3j and create proper mnemonic
        byte[] initialEntropy = new byte[16];
        secureRandom.nextBytes(initialEntropy);
//        String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);
        String mnemonic = new BigInteger(initialEntropy).abs().toString(16);

        // Create credentials from password and mnemonic
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        ECKeyPair ecKeyPair = ECKeyPair.create(sha256(seed));
        Credentials credentials = Credentials.create(ecKeyPair);
        String address = credentials.getAddress();

        WalletEntity walletEntity = repository.save(new WalletEntity(address));

        // Share address, mnemonic, and private key to user
        return new WalletCreateResponse(
                walletEntity.getId(), walletEntity.getAddress(), mnemonic, ecKeyPair.getPrivateKey().toString(16)
        );
    }

    @GetMapping("/balance")
    public WalletBalanceResponse getAccountBalance(@Param("address") String address) throws IOException {
        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        Optional<WalletEntity> optionalWalletEntity = repository.findByAddress(address);
        if (optionalWalletEntity.isEmpty()){
            throw new RuntimeException("Not present");
        }
        WalletEntity walletEntity = optionalWalletEntity.get();

        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        BigDecimal balanceInEth = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER);

        BigDecimal balanceInDb = walletEntity.getBalance().stripTrailingZeros();
        if (balanceInEth.compareTo(balanceInDb) != 0){
            // TODO fill nonce and txs
            walletEntity.setBalance(balanceInEth);
            repository.save(walletEntity);
            logger.info(String.format("Updated balance of %s", address));
        }

        return new WalletBalanceResponse(
                address, balance.getBalance(), balanceInEth
        );
    }

}
