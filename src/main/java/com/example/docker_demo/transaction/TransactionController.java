package com.example.docker_demo.transaction;

import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

@RestController
@RequestMapping("transaction")
public class TransactionController {
    @PostMapping("/send")
    public String send(@RequestBody TransactionSendRequest request) throws Exception {
        BigInteger privateKey = new BigInteger(request.getPrivateKeyHex(), 16);
        BigDecimal amount = request.getAmountAsDecimal();
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials credentials = Credentials.create(ecKeyPair);

        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        TransactionReceipt transferReceipt = Transfer.sendFunds(
                web3j, credentials, request.getToAddress(), amount, Convert.Unit.ETHER
        ).send();

        return ("Transaction complete, view it at https://ropsten.etherscan.io/tx/"
                + transferReceipt.getTransactionHash());
    }


    @GetMapping("/info")
    public TransactionInfoResponse info(@Param("address") String address, @Param("privateKeyHex") String privateKeyHex, @Param("transactionHash") String transactionHash) throws Exception {
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials credentials = Credentials.create(ecKeyPair);

        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        // TODO check credentials
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
        Transaction tx = ethTransaction.getTransaction().orElseThrow();


        return new TransactionInfoResponse(
                tx.getFrom(), tx.getTo(), tx.getValue(), tx.getGasPrice(), tx.getHash(), tx.getNonceRaw(), tx.getBlockHash(), tx.getBlockNumber(),
                "...", "https://ropsten.etherscan.io/tx/" + tx.getHash()
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
