package com.example.docker_demo.wallet;

import org.web3j.crypto.ECKeyPair;

import java.math.BigInteger;

public class Wallet {
    // TODO:
    //  private key salt
    //  nonce or transaction count or latest block number
    //  version and id
    private Integer id;
    private String address;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
