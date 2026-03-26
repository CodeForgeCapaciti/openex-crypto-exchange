package com.cryptoBackend.backend.controller;

import com.cryptoBackend.backend.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/orderbook")
    public ResponseEntity<Map<String, Object>> getOrderBook(@RequestParam(defaultValue = "BTC/USD") String symbol) {
        return ResponseEntity.ok(marketDataService.getOrderBook(symbol));
    }

    @GetMapping("/ticker")
    public ResponseEntity<Map<String, Object>> getTicker(@RequestParam(defaultValue = "BTC/USD") String symbol) {
        return ResponseEntity.ok(marketDataService.getTicker(symbol));
    }

    @GetMapping("/trades")
    public ResponseEntity<List<Map<String, Object>>> getRecentTrades(@RequestParam(defaultValue = "BTC/USD") String symbol) {
        return ResponseEntity.ok(marketDataService.getRecentTrades(symbol));
    }
}