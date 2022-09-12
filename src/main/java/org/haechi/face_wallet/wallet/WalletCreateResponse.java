package org.haechi.face_wallet.wallet;


public class WalletCreateResponse {
    private final Long id;
    private final String address;
    private final String mnemonic;
    private final String privateKeyHex;

    public WalletCreateResponse(Long id, String address, String mnemonic, String privateKeyHex) {
        this.id = id;
        this.address = address;
        this.mnemonic = mnemonic;
        this.privateKeyHex = privateKeyHex;
    }

    public Long getId() { return id; }

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
