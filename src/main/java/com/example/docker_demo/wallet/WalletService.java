package com.example.docker_demo.wallet;

import com.example.docker_demo.utils;

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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.web3j.crypto.Hash.sha256;

@Service
public class WalletService extends utils {
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

    public WalletEntity fetchWalletEntityFromAddressFromNode(String address) {
        EthGetBalance ethGetBalance = send(web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST), "fetching account balance from");
        EthGetTransactionCount ethGetTransactionCount = send(web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST), "fetching nonce from");
        BigDecimal balance = Convert.fromWei(new BigDecimal(ethGetBalance.getBalance()), Convert.Unit.ETHER);
        Long nonce = ethGetTransactionCount.getTransactionCount().longValue();

        Optional<WalletEntity> optionalWalletEntity = repository.findByAddress(address);
        if (optionalWalletEntity.isEmpty()) {
            WalletEntity walletEntity = new WalletEntity(address, nonce, balance, BigDecimal.ZERO);
            return repository.save(walletEntity);
        }
        else {
            WalletEntity walletEntity = optionalWalletEntity.get();
            if (walletEntity.getBalanceOnHold().compareTo(BigDecimal.ZERO) != 0) {
                return walletEntity;
            }
            else {
                walletEntity.setBalance(balance);
                walletEntity.setNonce(nonce);
                return repository.save(walletEntity);
            }
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

    public boolean isSufficientBalance(WalletEntity walletEntity, BigDecimal amount) {
        BigDecimal balanceAvailable = walletEntity.getBalance().subtract(walletEntity.getBalanceOnHold());
        return balanceAvailable.compareTo(amount) >= 0;
    }

    public WalletEntity addPendingTransaction(WalletEntity walletEntity, BigDecimal amount) {
        walletEntity.setBalanceOnHold(walletEntity.getBalanceOnHold().add(amount));
        walletEntity.setNonce(walletEntity.getNonce() + 1);
        return repository.save(walletEntity);
    }

    public Iterable<WalletEntity> updateConfirmedTransaction(WalletEntity fromWalletEntity, WalletEntity toWalletEntity, BigDecimal amount) {
        fromWalletEntity.setBalanceOnHold(fromWalletEntity.getBalanceOnHold().subtract(amount));
        fromWalletEntity.setBalance(fromWalletEntity.getBalance().subtract(amount));
        toWalletEntity.setBalance(toWalletEntity.getBalance().add(amount));
        return repository.saveAll(List.of(fromWalletEntity, toWalletEntity));
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

    @Override
    public void logWeb3Action(String msg) {
        logger.info(msg + " the node");
    }
}
