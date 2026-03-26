package com.cryptoBackend.backend.service;

import com.cryptoBackend.backend.dto.OrderRequest;
import com.cryptoBackend.backend.model.Order;
import com.cryptoBackend.backend.model.User;
import com.cryptoBackend.backend.model.Wallet;
import com.cryptoBackend.backend.repository.OrderRepository;
import com.cryptoBackend.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final MatchingEngineService matchingEngine;

    @Transactional
    public Order placeOrder(User user, OrderRequest request) {
        // Validate order
        validateOrder(user, request);

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setSymbol(request.getSymbol());
        order.setType(request.getType());
        order.setSide(request.getSide());
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());

        // Freeze funds
        freezeFunds(user, order);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Process order through matching engine
        matchingEngine.processOrder(savedOrder);

        return savedOrder;
    }

    private void validateOrder(User user, OrderRequest request) {
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Quantity must be positive");
        }

        if (request.getType() == Order.OrderType.LIMIT && request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Price must be positive for limit orders");
        }

        // Check balance
        if (request.getSide() == Order.Side.BUY) {
            BigDecimal requiredAmount = request.getType() == Order.OrderType.LIMIT ?
                    request.getPrice().multiply(request.getQuantity()) :
                    request.getQuantity(); // For market orders, simplified
            Wallet usdWallet = walletRepository.findByUserIdAndCurrency(user.getId(), "USD")
                    .orElseThrow(() -> new RuntimeException("USD wallet not found"));

            if (usdWallet.getAvailableBalance().compareTo(requiredAmount) < 0) {
                throw new RuntimeException("Insufficient USD balance");
            }
        } else {
            Wallet btcWallet = walletRepository.findByUserIdAndCurrency(user.getId(), "BTC")
                    .orElseThrow(() -> new RuntimeException("BTC wallet not found"));

            if (btcWallet.getAvailableBalance().compareTo(request.getQuantity()) < 0) {
                throw new RuntimeException("Insufficient BTC balance");
            }
        }
    }

    private void freezeFunds(User user, Order order) {
        if (order.getSide() == Order.Side.BUY) {
            BigDecimal requiredAmount = order.getType() == Order.OrderType.LIMIT ?
                    order.getPrice().multiply(order.getQuantity()) :
                    order.getQuantity();

            Wallet usdWallet = walletRepository.findByUserIdAndCurrency(user.getId(), "USD")
                    .orElseThrow(() -> new RuntimeException("USD wallet not found"));

            usdWallet.setAvailableBalance(usdWallet.getAvailableBalance().subtract(requiredAmount));
            usdWallet.setFrozenBalance(usdWallet.getFrozenBalance().add(requiredAmount));
            walletRepository.save(usdWallet);
        } else {
            Wallet btcWallet = walletRepository.findByUserIdAndCurrency(user.getId(), "BTC")
                    .orElseThrow(() -> new RuntimeException("BTC wallet not found"));

            btcWallet.setAvailableBalance(btcWallet.getAvailableBalance().subtract(order.getQuantity()));
            btcWallet.setFrozenBalance(btcWallet.getFrozenBalance().add(order.getQuantity()));
            walletRepository.save(btcWallet);
        }
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public void cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.PARTIALLY_FILLED) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        // Unfreeze funds
        if (order.getSide() == Order.Side.BUY) {
            BigDecimal frozenAmount = order.getType() == Order.OrderType.LIMIT ?
                    order.getPrice().multiply(order.getRemainingQuantity()) :
                    order.getRemainingQuantity();

            Wallet usdWallet = walletRepository.findByUserIdAndCurrency(userId, "USD")
                    .orElseThrow(() -> new RuntimeException("USD wallet not found"));

            usdWallet.setAvailableBalance(usdWallet.getAvailableBalance().add(frozenAmount));
            usdWallet.setFrozenBalance(usdWallet.getFrozenBalance().subtract(frozenAmount));
            walletRepository.save(usdWallet);
        } else {
            Wallet btcWallet = walletRepository.findByUserIdAndCurrency(userId, "BTC")
                    .orElseThrow(() -> new RuntimeException("BTC wallet not found"));

            btcWallet.setAvailableBalance(btcWallet.getAvailableBalance().add(order.getRemainingQuantity()));
            btcWallet.setFrozenBalance(btcWallet.getFrozenBalance().subtract(order.getRemainingQuantity()));
            walletRepository.save(btcWallet);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Cancelled order: {}", orderId);
    }
}