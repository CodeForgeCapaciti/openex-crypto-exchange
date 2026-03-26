package com.cryptoBackend.backend.service;


import com.cryptoBackend.backend.model.Order;
import com.cryptoBackend.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private BigDecimal currentPrice = new BigDecimal("50000");
    private final Map<String, Set<String>> subscriptions = new HashMap<>();

    @Scheduled(fixedDelay = 1000)
    public void simulateMarketData() {
        // Simulate price movement
        double change = (ThreadLocalRandom.current().nextDouble() - 0.5) * 100;
        BigDecimal priceChange = BigDecimal.valueOf(change);
        currentPrice = currentPrice.add(priceChange).max(BigDecimal.ONE);

        // Generate random trades for simulation
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            simulateRandomTrade();
        }

        // Store in Redis for caching
        Map<String, Object> ticker = new HashMap<>();
        ticker.put("price", currentPrice);
        ticker.put("timestamp", LocalDateTime.now().toString());
        ticker.put("volume24h", BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble() * 1000));
        ticker.put("change24h", BigDecimal.valueOf((ThreadLocalRandom.current().nextDouble() - 0.5) * 10));

        redisTemplate.opsForValue().set("market:btc_usd:ticker", ticker);
    }

    private void simulateRandomTrade() {
        BigDecimal quantity = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble() * 0.5);
        BigDecimal price = currentPrice.add(BigDecimal.valueOf(
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 100));

        Map<String, Object> trade = new HashMap<>();
        trade.put("price", price);
        trade.put("quantity", quantity);
        trade.put("timestamp", LocalDateTime.now().toString());
        trade.put("side", ThreadLocalRandom.current().nextBoolean() ? "buy" : "sell");

        redisTemplate.opsForList().leftPush("market:btc_usd:trades", trade);
        redisTemplate.opsForList().trim("market:btc_usd:trades", 0, 99);

        log.debug("Simulated trade: {} BTC at ${}", quantity, price);
    }

    public Map<String, Object> getOrderBook(String symbol) {
        List<Order> activeOrders = orderRepository.findActiveOrdersBySymbol(symbol,
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.PARTIALLY_FILLED));

        Map<BigDecimal, BigDecimal> bids = new TreeMap<>(Collections.reverseOrder());
        Map<BigDecimal, BigDecimal> asks = new TreeMap<>();

        for (Order order : activeOrders) {
            if (order.getSide() == Order.Side.BUY) {
                bids.merge(order.getPrice(), order.getRemainingQuantity(), BigDecimal::add);
            } else {
                asks.merge(order.getPrice(), order.getRemainingQuantity(), BigDecimal::add);
            }
        }

        List<List<BigDecimal>> bidList = new ArrayList<>();
        List<List<BigDecimal>> askList = new ArrayList<>();

        bids.forEach((price, quantity) ->
                bidList.add(List.of(price, quantity)));
        asks.forEach((price, quantity) ->
                askList.add(List.of(price, quantity)));

        Map<String, Object> result = new HashMap<>();
        result.put("bids", bidList);
        result.put("asks", askList);
        result.put("timestamp", LocalDateTime.now());

        return result;
    }

    public Map<String, Object> getTicker(String symbol) {
        Map<String, Object> ticker = (Map<String, Object>) redisTemplate.opsForValue()
                .get("market:btc_usd:ticker");
        return ticker != null ? ticker : new HashMap<>();
    }

    public List<Map<String, Object>> getRecentTrades(String symbol) {

        List<Object> rawTrades = redisTemplate.opsForList()
                .range("market:btc_usd:trades", 0, 49);

        if (rawTrades == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> trades = new ArrayList<>();

        for (Object obj : rawTrades) {
            if (obj instanceof Map) {
                trades.add((Map<String, Object>) obj);
            }
        }

        return trades;
    }

    public void subscribe(String sessionId, String symbol) {
        subscriptions.computeIfAbsent(symbol, k -> new HashSet<>()).add(sessionId);
    }

    public void unsubscribe(String sessionId) {
        subscriptions.values().forEach(set -> set.remove(sessionId));
    }
}