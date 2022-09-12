package org.haechi.face_wallet.wallet;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class WalletCreateRequest {
    @NotNull(message = "Password must be between 4 and 32 characters")
    @Size(min = 4, max = 32)
    private String password;

    public WalletCreateRequest() {}

    public WalletCreateRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
