package com.example.docker_demo;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.methods.response.NewAccountIdentifier;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootApplication
@RestController
public class DockerDemoApplication {
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(64000);
        return loggingFilter;
    }

    @RequestMapping("/")
    public String home() {
        return "Hello World";
    }

    @GetMapping("/send")
    public String send(@Param("toAddress") String toAddress, @Param("publicKeyHex") String publicKeyHex, @Param("privateKeyHex") String privateKeyHex) throws Exception {
        ///http://localhost:8080/send
        // &toAddress=0x188051d883991C0D7685aEAF7D3f0F7089cE60b5
        // &publicKey=71a5dae54184e159b980597e814736c2f9c75fdaf0a440e056c44123875d0ae813dddd19bec9b92350a0302496f01b25bf37335dcc9c9b204efc042c20ba8a6a
        // &privateKey=a11371ac2dcfbc71f1599d901b098b782cff271e54707b0a80a8ce0735e1f493
        System.out.println("a" + new BigInteger("123", 16).toString());
        System.out.printf("pub: %s, pri: %s%n", publicKeyHex, privateKeyHex);
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        BigInteger publicKey = new BigInteger(publicKeyHex, 16);
        ECKeyPair ecKeyPair = new ECKeyPair(privateKey, publicKey);
        Credentials credentials = Credentials.create(ecKeyPair);

        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        TransactionReceipt transferReceipt = Transfer.sendFunds(
                web3j, credentials, toAddress, new BigDecimal("0.1"), Convert.Unit.ETHER
        ).send();

        return ("Transaction complete, view it at https://ropsten.etherscan.io/tx/"
                + transferReceipt.getTransactionHash());
    }

    @RequestMapping("/version")
    public String version() throws IOException {
        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));
        Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
        return web3ClientVersion.getWeb3ClientVersion();
    }

    public static void main(String[] args) {
        SpringApplication.run(DockerDemoApplication.class, args);
    }

}
