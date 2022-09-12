package com.example.docker_demo.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;

import static org.web3j.crypto.Hash.sha256;

@Service
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class.getName());
    private static final SecureRandom secureRandom = new SecureRandom();
    private final WalletRepository repository;
    private final Validator validator;

    private final Web3j web3j = Web3j.build(new HttpService(
            "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

    public WalletService(WalletRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    // Web3 methods
    public String generateRandomMnemonic() {
        byte[] initialEntropy = new byte[16];
        secureRandom.nextBytes(initialEntropy);
        // TODO update web3j and create proper mnemonic
        // String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);
        return new BigInteger(initialEntropy).abs().toString(16);
    }

    public Credentials generateCredentialsFromMnemonicAndPassword(String mnemonic, String password) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        ECKeyPair ecKeyPair = ECKeyPair.create(sha256(seed));
        return Credentials.create(ecKeyPair);
    }

    public WalletEntity updateWalletInfo(WalletEntity walletEntity) {
        String address = walletEntity.getAddress();

        BigInteger nonce;
        BigDecimal balanceInEth;
        try {
            // Fetch nonce and balance from the node
            // TODO make this atomic?
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
            nonce = ethGetTransactionCount.getTransactionCount();
            EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            balanceInEth = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER);
        }
        catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while fetching wallet info from the node. Is the node down?"
            );
        }
        if (walletEntity.getNonce() != nonce.longValue() || balanceInEth.compareTo(walletEntity.getBalance()) != 0) {
            walletEntity.setNonce(nonce.longValue());
            walletEntity.setBalance(balanceInEth);
            return repository.save(walletEntity);
        }
        else {
            return walletEntity;
        }
    }


    // DB methods
    public WalletEntity createWalletEntityFromCredentials(Credentials credentials) {
        String address = credentials.getAddress().toLowerCase();
        return repository.save(new WalletEntity(address));
    }

    public WalletEntity fetchWalletEntityFromAddress(String address) {
        return repository.findByAddress(address).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "wallet")
        );
    }

    public WalletEntity increaseWalletEntityNonce(WalletEntity walletEntity) {
        walletEntity.setNonce(walletEntity.getNonce() + 1);
        return repository.save(walletEntity);
    }

    public WalletEntity setWalletEntityBalance(WalletEntity walletEntity, BigDecimal balance) {
        walletEntity.setBalance(balance);
        return repository.save(walletEntity);
    }

    public boolean tryIncreaseWalletEntityBalance(String address, BigDecimal amount) {
        Optional<WalletEntity> optionalEntity = repository.findByAddress(address);
        if (optionalEntity.isEmpty()){
            return false;
        }
        WalletEntity walletEntity = optionalEntity.get();
        walletEntity.setBalance(walletEntity.getBalance().add(amount));
        repository.save(walletEntity);
        return true;
    }

    // DTO methods
    public boolean validateCreateRequest(WalletCreateRequest request) {
        Set<ConstraintViolation<WalletCreateRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<WalletCreateRequest> constraintViolation : violations) {
                sb.append(constraintViolation.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, sb.toString());
        }

        return true;
    }

    public WalletCreateResponse createWalletCreateResponse(WalletEntity walletEntity, String mnemonic, Credentials credentials) {
        return new WalletCreateResponse(
                walletEntity.getId(),
                walletEntity.getAddress(),
                mnemonic,
                credentials.getEcKeyPair().getPrivateKey().toString(16)
        );
    }
}
