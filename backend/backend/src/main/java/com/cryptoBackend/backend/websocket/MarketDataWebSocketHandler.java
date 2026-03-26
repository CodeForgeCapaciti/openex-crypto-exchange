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
public class MarketDataWebSocketHandler extends TextWebSocketHandler {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("New WebSocket connection established: {}", session.getId());

        // Send initial market data
        sendInitialMarketData(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", session.getId(), payload);

        try {
            Map<String, String> request = objectMapper.readValue(payload, Map.class);
            String type = request.get("type");
            String symbol = request.get("symbol");

            if ("subscribe".equals(type) && symbol != null) {
                marketDataService.subscribe(session.getId(), symbol);
                log.info("Session {} subscribed to {}", session.getId(), symbol);
            } else if ("unsubscribe".equals(type) && symbol != null) {
                marketDataService.unsubscribe(session.getId());
                log.info("Session {} unsubscribed", session.getId());
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            session.sendMessage(new TextMessage("Error processing message: " + e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        marketDataService.unsubscribe(session.getId());
        log.info("WebSocket connection closed: {} - {}", session.getId(), status);
    }

    private void sendInitialMarketData(WebSocketSession session) throws IOException {
        Map<String, Object> initialData = Map.of(
                "type", "initial_data",
                "orderBook", marketDataService.getOrderBook("BTC/USD"),
                "ticker", marketDataService.getTicker("BTC/USD"),
                "trades", marketDataService.getRecentTrades("BTC/USD")
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initialData)));
    }

    public void broadcastMarketData(String symbol, Object data) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "market_data",
                    "symbol", symbol,
                    "data", data
            ));

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error broadcasting market data", e);
        }
    }
}