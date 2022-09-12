package com.example.docker_demo;

import com.example.docker_demo.wallet.WalletController;
import com.example.docker_demo.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
public class WalletCreationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletRepository walletRepository;

    @Test
    public void checkExistingWalletForDemo() throws Exception {
        String address = "0x82ac8f02278b2c9ca5ffe3483599b0b70c609d59";
        this.mockMvc.perform(get("/info?address=" + address))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", is(address)))
                .andExpect(jsonPath("$.balanceInEth", greaterThanOrEqualTo(0)));

        verify(walletRepository).findByAddress(address);
    }
}
