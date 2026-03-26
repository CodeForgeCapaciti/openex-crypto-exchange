package com.cryptoBackend.backend.repository;

import com.cryptoBackend.backend.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUserId(String userId);
    Optional<Wallet> findByUserIdAndCurrency(String userId, String currency);

    @Modifying
    @Transactional
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, w.availableBalance = w.availableBalance + :amount WHERE w.user.id = :userId AND w.currency = :currency")
    int addBalance(@Param("userId") String userId, @Param("currency") String currency, @Param("amount") BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount, w.availableBalance = w.availableBalance - :amount WHERE w.user.id = :userId AND w.currency = :currency AND w.balance >= :amount")
    int subtractBalance(@Param("userId") String userId, @Param("currency") String currency, @Param("amount") BigDecimal amount);
}