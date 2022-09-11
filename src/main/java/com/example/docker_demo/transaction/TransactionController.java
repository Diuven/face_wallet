package com.example.docker_demo.transaction;

import com.example.docker_demo.wallet.WalletEntity;
import com.example.docker_demo.wallet.WalletRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

@RestController
@RequestMapping("transaction")
public class TransactionController {
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(21000);
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionController(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<TransactionEntity> send(@RequestBody TransactionSendRequest request) throws Exception {
        BigInteger privateKey = new BigInteger(request.getPrivateKeyHex(), 16);
        BigDecimal amount = request.getAmountAsDecimal();
        String toAddress = request.getToAddress().toLowerCase();
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials credentials = Credentials.create(ecKeyPair);
        String fromAddress = credentials.getAddress().toLowerCase();

        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));


        // Get balance and nonce of fromAddress, and update db if necessary
        // TODO make this atmoic?
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).send();
        BigInteger nonce =  ethGetTransactionCount.getTransactionCount();
        EthGetBalance ethGetBalance = web3j.ethGetBalance(fromAddress, DefaultBlockParameterName.LATEST).send();
        BigDecimal balanceInEth = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);

        Optional<WalletEntity> optionalWalletEntity = walletRepository.findByAddress(fromAddress);
        WalletEntity walletEntity = (optionalWalletEntity.orElseGet(
                () -> new WalletEntity(fromAddress, nonce.longValue(), balanceInEth))
        );
        if (walletEntity.getBalance().compareTo(balanceInEth) != 0 || walletEntity.getNonce() != nonce.longValue()){
            walletEntity.setBalance(balanceInEth);
            walletEntity.setNonce(nonce.longValue());
            walletEntity = walletRepository.save(walletEntity);
        }

        if (balanceInEth.compareTo(amount) < 0) {
            // Reject if balance is not enough
            throw new RuntimeException(String.format("balance is not enough (%s > %s)", amount, balanceInEth));
        }

        // TODO set gas priority from request?
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        // Prepare the rawTransaction
        RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(
                nonce, gasPrice, GAS_LIMIT, toAddress, Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger());

        // Sign the transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        // Write in db
        TransactionEntity transactionEntity = new TransactionEntity(
                fromAddress, toAddress, amount, nonce.longValue()
        );
        transactionEntity = transactionRepository.save(transactionEntity);

        // Send transaction
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        String transactionHash = ethSendTransaction.getTransactionHash();

        // Wait for transaction to be mined
        Optional<TransactionReceipt> optionalTransactionReceipt = Optional.empty();
        do {
            EthGetTransactionReceipt ethGetTransactionReceiptResp = web3j.ethGetTransactionReceipt(transactionHash).send();
            optionalTransactionReceipt = ethGetTransactionReceiptResp.getTransactionReceipt();
            Thread.sleep(3000); // Wait 3 sec
        } while(optionalTransactionReceipt.isEmpty());
        TransactionReceipt receipt = optionalTransactionReceipt.get();

        if (!receipt.getStatus().equals("0x1")){
            throw new RuntimeException(String.format("Transaction not successful %s", transactionHash));
        }

        transactionEntity.setStatus("MINED");
        transactionEntity.setTransactionHash(transactionHash);
        transactionEntity.setBlockHash(receipt.getBlockHash());
        transactionEntity.setBlockNumber(receipt.getBlockNumber());
        transactionEntity = transactionRepository.save(transactionEntity);

        walletEntity.setNonce(nonce.longValue() + 1);
        walletEntity = walletRepository.save(walletEntity);


        int blockConfirmation = 0;
        do {
            EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();
            BigInteger rawBlockConfirmation = ethBlockNumber.getBlockNumber().subtract(receipt.getBlockNumber());
            blockConfirmation = Math.min(rawBlockConfirmation.intValue(), 12);
            Thread.sleep(5000); // Wait 3 sec
        } while(blockConfirmation < 12);

        transactionEntity.setBlockConfirmation(blockConfirmation);
        transactionEntity.setStatus("CONFIRMED");
        transactionEntity = transactionRepository.save(transactionEntity);

        walletEntity.setBalance(walletEntity.getBalance().subtract(amount));
        walletEntity = walletRepository.save(walletEntity);

        return ResponseEntity.ok(transactionEntity);
    }

    // TODO make async send


    @GetMapping("/info")
    public TransactionInfoResponse info(@Param("address") String address, @Param("privateKeyHex") String privateKeyHex, @Param("transactionHash") String transactionHash) throws Exception {
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials credentials = Credentials.create(ecKeyPair);

        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        EthGetTransactionReceipt ethGetTransactionReceiptResp = web3j.ethGetTransactionReceipt(transactionHash).send();
        TransactionReceipt transactionReceipt = ethGetTransactionReceiptResp.getTransactionReceipt().orElseThrow();


        // TODO check credentials
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
        Transaction tx = ethTransaction.getTransaction().orElseThrow();


        return new TransactionInfoResponse(
                tx.getFrom(), tx.getTo(), tx.getValue(), tx.getGasPrice(), tx.getHash(), tx.getNonceRaw(), tx.getBlockHash(), tx.getBlockNumber(),
                transactionReceipt.getStatus(), "https://ropsten.etherscan.io/tx/" + tx.getHash()
        );
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
