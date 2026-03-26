package com.cryptoBackend.backend.service;



import com.cryptoBackend.backend.dto.WalletResponse;
import com.cryptoBackend.backend.model.User;
import com.cryptoBackend.backend.model.Wallet;
import com.cryptoBackend.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;

    public List<WalletResponse> getUserWallets(String userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        log.debug("Found {} wallets for user {}", wallets.size(), userId);
        return wallets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBalance(String userId, String currency, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            int updated = walletRepository.addBalance(userId, currency, amount);
            if (updated == 0) {
                throw new RuntimeException("Failed to add balance - wallet may not exist");
            }
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            int updated = walletRepository.subtractBalance(userId, currency, amount.abs());
            if (updated == 0) {
                throw new RuntimeException("Insufficient balance or wallet not found");
            }
        }
        log.debug("Updated balance for user {}: {} {}", userId, amount, currency);
    }

    @Transactional
    public void deposit(String userId, String currency, String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        Optional<Wallet> walletOpt = walletRepository.findByUserIdAndCurrency(userId, currency);
        if (walletOpt.isEmpty()) {
            log.error("Wallet not found for userId={} and currency={}", userId, currency);
            throw new RuntimeException("Wallet not found for " + currency + ". Please create wallet first via /api/wallets/create");
        }

        int updated = walletRepository.addBalance(userId, currency, amount);
        if (updated == 0) {
            throw new RuntimeException("Failed to deposit - wallet not found");
        }
        log.info("Deposited {} {} to user {}", amount, currency, userId);
    }

    @Transactional
    public WalletResponse createWallet(String userId, String currency, BigDecimal initialBalance) {
        log.info("Creating wallet for userId: {}, currency: {}, initialBalance: {}", userId, currency, initialBalance);

        // Check if wallet already exists
        Optional<Wallet> existingWallet = walletRepository.findByUserIdAndCurrency(userId, currency.toUpperCase());
        if (existingWallet.isPresent()) {
            throw new RuntimeException("Wallet for " + currency + " already exists with balance: " + existingWallet.get().getBalance());
        }

        // Get User entity
        User user = userService.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        log.info("Found user: id={}, email={}", user.getId(), user.getEmail());

        // Create new wallet
        Wallet wallet = new Wallet();
        wallet.setCurrency(currency.toUpperCase());
        wallet.setUser(user);
        wallet.setBalance(initialBalance);
        wallet.setAvailableBalance(initialBalance);
        wallet.setFrozenBalance(BigDecimal.ZERO);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Created wallet {} for user {} with balance {}", currency, userId, initialBalance);

        return toResponse(savedWallet);
    }

    private WalletResponse toResponse(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setCurrency(wallet.getCurrency());
        response.setBalance(wallet.getBalance());
        response.setAvailableBalance(wallet.getAvailableBalance());
        response.setFrozenBalance(wallet.getFrozenBalance());
        return response;
    }
}