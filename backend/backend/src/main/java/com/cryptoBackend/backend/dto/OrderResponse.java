package com.cryptoBackend.backend.dto;

import com.cryptoBackend.backend.model.Order;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private String id;
    private String symbol;
    private Order.OrderType type;
    private Order.Side side;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private BigDecimal remainingQuantity;
    private Order.OrderStatus status;
    private LocalDateTime createdAt;

    public static OrderResponse fromOrder(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setSymbol(order.getSymbol());
        response.setType(order.getType());
        response.setSide(order.getSide());
        response.setPrice(order.getPrice());
        response.setQuantity(order.getQuantity());
        response.setFilledQuantity(order.getFilledQuantity());
        response.setRemainingQuantity(order.getRemainingQuantity());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
}