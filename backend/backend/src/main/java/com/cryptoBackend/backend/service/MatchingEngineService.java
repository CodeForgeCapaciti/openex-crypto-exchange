package com.cryptoBackend.backend.service;

import com.cryptoBackend.backend.model.Order;
import com.cryptoBackend.backend.model.Trade;
import com.cryptoBackend.backend.repository.OrderRepository;
import com.cryptoBackend.backend.repository.TradeRepository;
import com.cryptoBackend.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngineService {
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;

    // Order books for each symbol
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    @Transactional
    public List<Trade> processOrder(Order order) {
        log.info("Processing order: {} for user: {}", order.getId(), order.getUser().getId());

        OrderBook orderBook = orderBooks.computeIfAbsent(order.getSymbol(),
                k -> new OrderBook());

        List<Trade> trades = new ArrayList<>();

        if (order.getSide() == Order.Side.BUY) {
            trades = matchBuyOrder(order, orderBook);
        } else {
            trades = matchSellOrder(order, orderBook);
        }

        // Update order status
        if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(Order.OrderStatus.FILLED);
            // Unfreeze remaining funds
            unfreezeFunds(order);
        } else if (order.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(Order.OrderStatus.PARTIALLY_FILLED);
            // Partially unfreeze funds
            partialUnfreezeFunds(order);
        }

        orderRepository.save(order);

        // Add remaining quantity to order book if not fully filled
        if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 &&
                order.getStatus() != Order.OrderStatus.CANCELLED) {
            orderBook.addOrder(order);
        }

        // Save trades
        tradeRepository.saveAll(trades);

        return trades;
    }

    private List<Trade> matchBuyOrder(Order buyOrder, OrderBook orderBook) {
        List<Trade> trades = new ArrayList<>();
        PriorityBlockingQueue<Order> sellOrders = orderBook.getSellOrders();

        BigDecimal remainingQuantity = buyOrder.getRemainingQuantity();

        while (!sellOrders.isEmpty() && remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            Order sellOrder = sellOrders.peek();

            // For limit orders, check price
            if (buyOrder.getType() == Order.OrderType.LIMIT &&
                    buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                break;
            }

            sellOrder = sellOrders.poll();
            BigDecimal tradeQuantity = remainingQuantity.min(sellOrder.getRemainingQuantity());
            BigDecimal tradePrice = sellOrder.getPrice();

            if (buyOrder.getType() == Order.OrderType.MARKET) {
                tradePrice = sellOrder.getPrice();
            }

            // Execute trade
            Trade trade = executeTrade(buyOrder, sellOrder, tradePrice, tradeQuantity);
            trades.add(trade);

            // Update quantities
            remainingQuantity = remainingQuantity.subtract(tradeQuantity);
            buyOrder.setFilledQuantity(buyOrder.getFilledQuantity().add(tradeQuantity));
            buyOrder.setRemainingQuantity(remainingQuantity);

            // Update sell order
            sellOrder.setFilledQuantity(sellOrder.getFilledQuantity().add(tradeQuantity));
            sellOrder.setRemainingQuantity(sellOrder.getRemainingQuantity().subtract(tradeQuantity));

            if (sellOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                sellOrders.offer(sellOrder);
            } else {
                sellOrder.setStatus(Order.OrderStatus.FILLED);
                orderRepository.save(sellOrder);
                unfreezeFunds(sellOrder);
            }
        }

        return trades;
    }

    private List<Trade> matchSellOrder(Order sellOrder, OrderBook orderBook) {
        List<Trade> trades = new ArrayList<>();
        PriorityBlockingQueue<Order> buyOrders = orderBook.getBuyOrders();

        BigDecimal remainingQuantity = sellOrder.getRemainingQuantity();

        while (!buyOrders.isEmpty() && remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            Order buyOrder = buyOrders.peek();

            // For limit orders, check price
            if (sellOrder.getType() == Order.OrderType.LIMIT &&
                    sellOrder.getPrice().compareTo(buyOrder.getPrice()) > 0) {
                break;
            }

            buyOrder = buyOrders.poll();
            BigDecimal tradeQuantity = remainingQuantity.min(buyOrder.getRemainingQuantity());
            BigDecimal tradePrice = buyOrder.getPrice();

            if (sellOrder.getType() == Order.OrderType.MARKET) {
                tradePrice = buyOrder.getPrice();
            }

            // Execute trade
            Trade trade = executeTrade(buyOrder, sellOrder, tradePrice, tradeQuantity);
            trades.add(trade);

            // Update quantities
            remainingQuantity = remainingQuantity.subtract(tradeQuantity);
            sellOrder.setFilledQuantity(sellOrder.getFilledQuantity().add(tradeQuantity));
            sellOrder.setRemainingQuantity(remainingQuantity);

            // Update buy order
            buyOrder.setFilledQuantity(buyOrder.getFilledQuantity().add(tradeQuantity));
            buyOrder.setRemainingQuantity(buyOrder.getRemainingQuantity().subtract(tradeQuantity));

            if (buyOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                buyOrders.offer(buyOrder);
            } else {
                buyOrder.setStatus(Order.OrderStatus.FILLED);
                orderRepository.save(buyOrder);
                unfreezeFunds(buyOrder);
            }
        }

        return trades;
    }

    private Trade executeTrade(Order buyOrder, Order sellOrder, BigDecimal price, BigDecimal quantity) {
        log.info("Executing trade: {} BTC at ${} between orders {} and {}",
                quantity, price, buyOrder.getId(), sellOrder.getId());

        Trade trade = new Trade();
        trade.setBuyOrder(buyOrder);
        trade.setSellOrder(sellOrder);
        trade.setPrice(price);
        trade.setQuantity(quantity);

        // Calculate fee (0.1%)
        BigDecimal fee = price.multiply(quantity).multiply(new BigDecimal("0.001"));
        trade.setFee(fee);

        // Update wallets
        BigDecimal totalValue = price.multiply(quantity);

        // Buyer pays USD, receives BTC
        updateWalletBalance(buyOrder.getUser().getId(), "USD", totalValue.negate());
        updateWalletBalance(buyOrder.getUser().getId(), "BTC", quantity);

        // Seller receives USD (minus fee), gives BTC
        updateWalletBalance(sellOrder.getUser().getId(), "USD", totalValue.subtract(fee));
        updateWalletBalance(sellOrder.getUser().getId(), "BTC", quantity.negate());

        return trade;
    }

    private void updateWalletBalance(String userId, String currency, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            walletRepository.addBalance(userId, currency, amount);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            walletRepository.subtractBalance(userId, currency, amount.abs());
        }
    }

    private void unfreezeFunds(Order order) {
        if (order.getSide() == Order.Side.BUY) {
            BigDecimal frozenAmount = order.getType() == Order.OrderType.LIMIT ?
                    order.getPrice().multiply(order.getRemainingQuantity()) :
                    order.getRemainingQuantity();

            walletRepository.findByUserIdAndCurrency(order.getUser().getId(), "USD")
                    .ifPresent(wallet -> {
                        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(frozenAmount));
                        walletRepository.save(wallet);
                    });
        } else {
            walletRepository.findByUserIdAndCurrency(order.getUser().getId(), "BTC")
                    .ifPresent(wallet -> {
                        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(order.getRemainingQuantity()));
                        walletRepository.save(wallet);
                    });
        }
    }

    private void partialUnfreezeFunds(Order order) {
        if (order.getSide() == Order.Side.BUY) {
            BigDecimal totalFrozen = order.getType() == Order.OrderType.LIMIT ?
                    order.getPrice().multiply(order.getQuantity()) :
                    order.getQuantity();
            BigDecimal usedAmount = order.getPrice().multiply(order.getFilledQuantity());
            BigDecimal toUnfreeze = totalFrozen.subtract(usedAmount);

            walletRepository.findByUserIdAndCurrency(order.getUser().getId(), "USD")
                    .ifPresent(wallet -> {
                        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(toUnfreeze));
                        walletRepository.save(wallet);
                    });
        } else {
            BigDecimal toUnfreeze = order.getRemainingQuantity();
            walletRepository.findByUserIdAndCurrency(order.getUser().getId(), "BTC")
                    .ifPresent(wallet -> {
                        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(toUnfreeze));
                        walletRepository.save(wallet);
                    });
        }
    }

    // Inner class for order book
    private static class OrderBook {
        private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<>(11,
                (o1, o2) -> {
                    // Higher price first for buy orders
                    int priceCompare = o2.getPrice().compareTo(o1.getPrice());
                    if (priceCompare != 0) return priceCompare;
                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                });

        private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<>(11,
                (o1, o2) -> {
                    // Lower price first for sell orders
                    int priceCompare = o1.getPrice().compareTo(o2.getPrice());
                    if (priceCompare != 0) return priceCompare;
                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                });

        public void addOrder(Order order) {
            if (order.getSide() == Order.Side.BUY) {
                buyOrders.offer(order);
            } else {
                sellOrders.offer(order);
            }
        }

        public PriorityBlockingQueue<Order> getBuyOrders() {
            return buyOrders;
        }

        public PriorityBlockingQueue<Order> getSellOrders() {
            return sellOrders;
        }
    }
}