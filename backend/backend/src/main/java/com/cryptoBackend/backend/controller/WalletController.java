package com.cryptoBackend.backend.controller;



import com.cryptoBackend.backend.dto.WalletResponse;
import com.cryptoBackend.backend.model.User;
import com.cryptoBackend.backend.service.UserService;
import com.cryptoBackend.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getWallets(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("GET /wallets called by: {}", userDetails.getUsername());
            User user = userService.findByEmail(userDetails.getUsername());
            List<WalletResponse> wallets = walletService.getUserWallets(user.getId());
            log.info("Found {} wallets for user {}", wallets.size(), user.getUsername());
            return ResponseEntity.ok(wallets);
        } catch (Exception e) {
            log.error("Error getting wallets", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestParam String currency,
                                     @RequestParam String amount,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("POST /wallets/deposit - User: {}, Currency: {}, Amount: {}",
                    userDetails.getUsername(), currency, amount);
            User user = userService.findByEmail(userDetails.getUsername());
            walletService.deposit(user.getId(), currency, amount);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully deposited " + amount + " " + currency);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error depositing funds", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createWallet(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestParam String currency,
                                          @RequestParam(defaultValue = "0") BigDecimal initialBalance) {
        try {
            log.info("POST /wallets/create - User: {}, Currency: {}, InitialBalance: {}",
                    userDetails.getUsername(), currency, initialBalance);

            // Get user entity
            User user = userService.findByEmail(userDetails.getUsername());
            log.info("Found user: id={}, email={}", user.getId(), user.getEmail());

            // Validate initial balance
            if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Initial balance cannot be negative");
            }

            // Call service
            WalletResponse walletResponse = walletService.createWallet(
                    user.getId(),
                    currency.toUpperCase(),
                    initialBalance
            );

            log.info("Wallet created successfully: {}", walletResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Wallet created successfully");
            response.put("wallet", walletResponse);
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating wallet", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}