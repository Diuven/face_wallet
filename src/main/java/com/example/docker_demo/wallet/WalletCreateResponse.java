package com.example.docker_demo.wallet;


public class WalletCreateResponse {
    private final String address;
    private final String mnemonic;
    private final String privateKeyHex;

    public WalletCreateResponse(String address, String mnemonic, String privateKeyHex) {
        this.address = address;
        this.mnemonic = mnemonic;
        this.privateKeyHex = privateKeyHex;
    }

    public String getAddress() {
        return address;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public String getPrivateKeyHex() {
        return privateKeyHex;
    }
}
