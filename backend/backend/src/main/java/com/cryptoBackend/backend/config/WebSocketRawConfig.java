package com.cryptoBackend.backend.config;

import com.cryptoBackend.backend.websocket.MarketDataWebSocketHandler;
import com.cryptoBackend.backend.websocket.OrderBookWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketRawConfig implements WebSocketConfigurer {

    private final MarketDataWebSocketHandler marketDataWebSocketHandler;
    private final OrderBookWebSocketHandler orderBookWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataWebSocketHandler, "/ws/market")
                .setAllowedOrigins("http://localhost:3000");

        registry.addHandler(orderBookWebSocketHandler, "/ws/orderbook")
                .setAllowedOrigins("http://localhost:3000");
    }
}