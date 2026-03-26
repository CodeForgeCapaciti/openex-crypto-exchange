package com.cryptoBackend.backend.websocket;


import com.cryptoBackend.backend.service.MarketDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBookWebSocketHandler extends TextWebSocketHandler {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionSymbols = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("OrderBook WebSocket connection established: {}", session.getId());

        // Send initial order book
        sendOrderBookUpdate(session, "BTC/USD");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", session.getId(), payload);

        try {
            Map<String, String> request = objectMapper.readValue(payload, Map.class);
            String symbol = request.get("symbol");

            if (symbol != null) {
                sessionSymbols.put(session.getId(), symbol);
                sendOrderBookUpdate(session, symbol);
            }
        } catch (Exception e) {
            log.error("Error processing OrderBook WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        sessionSymbols.remove(session.getId());
        log.info("OrderBook WebSocket connection closed: {}", session.getId());
    }

    private void sendOrderBookUpdate(WebSocketSession session, String symbol) throws IOException {
        Map<String, Object> orderBook = marketDataService.getOrderBook(symbol);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "orderbook_update",
                "symbol", symbol,
                "data", orderBook
        ))));
    }

    public void broadcastOrderBookUpdate(String symbol, Map<String, Object> orderBook) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "orderbook_update",
                    "symbol", symbol,
                    "data", orderBook
            ));

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    String subscribedSymbol = sessionSymbols.get(session.getId());
                    if (subscribedSymbol == null || subscribedSymbol.equals(symbol)) {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(message));
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error broadcasting order book update", e);
        }
    }
}