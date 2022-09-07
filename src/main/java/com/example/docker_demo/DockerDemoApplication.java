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

    @GetMapping("/wallet/balance")
    public String getAccountBalance(@Param("address") String address) throws IOException {
        Web3j web3j = Web3j.build(new HttpService(
                "https://tn.henesis.io/ethereum/ropsten?clientId=815fcd01324b8f75818a755a72557750"));

        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        BigDecimal balanceInEther = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER);

        return String.format("address: %s, balance: %s, in eth: %s", address, balance, balanceInEther.toString());
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
