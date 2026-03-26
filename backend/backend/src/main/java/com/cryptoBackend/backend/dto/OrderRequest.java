package com.cryptoBackend.backend.dto;




import com.cryptoBackend.backend.model.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    @NotBlank
    private String symbol;

    @NotNull
    private Order.OrderType type;

    @NotNull
    private Order.Side side;

    private BigDecimal price;

    @NotNull
    @Positive
    private BigDecimal quantity;
}