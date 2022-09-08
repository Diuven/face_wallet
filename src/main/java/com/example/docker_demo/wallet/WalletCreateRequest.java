package com.example.docker_demo.wallet;


public class WalletCreateRequest {
    private String password;

    public WalletCreateRequest() {}

    public WalletCreateRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
