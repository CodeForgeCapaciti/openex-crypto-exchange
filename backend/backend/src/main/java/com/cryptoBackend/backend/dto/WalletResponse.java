package com.cryptoBackend.backend.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletResponse {
    private String currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
}