package com.cryptoBackend.backend.service;


import com.cryptoBackend.backend.model.User;
import com.cryptoBackend.backend.model.Wallet;
import com.cryptoBackend.backend.repository.UserRepository;
import com.cryptoBackend.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String email, String password, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(username != null ? username : email.split("@")[0]);

        User savedUser = userRepository.save(user);
        initializeWallets(savedUser);
        log.info("Registered new user: {}", email);
        return savedUser;
    }

    @Transactional
    public void initializeWallets(User user) {
        // Create BTC wallet with 0 balance
        Wallet btcWallet = new Wallet();
        btcWallet.setUser(user);
        btcWallet.setCurrency("BTC");
        btcWallet.setBalance(BigDecimal.ZERO);
        btcWallet.setAvailableBalance(BigDecimal.ZERO);
        btcWallet.setFrozenBalance(BigDecimal.ZERO);
        walletRepository.save(btcWallet);

        // Create USD wallet with $10,000 for testing
        Wallet usdWallet = new Wallet();
        usdWallet.setUser(user);
        usdWallet.setCurrency("USD");
        usdWallet.setBalance(new BigDecimal("10000"));
        usdWallet.setAvailableBalance(new BigDecimal("10000"));
        usdWallet.setFrozenBalance(BigDecimal.ZERO);
        walletRepository.save(usdWallet);

        log.info("Initialized wallets for user: {} (BTC: 0, USD: 10000)", user.getUsername());
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}