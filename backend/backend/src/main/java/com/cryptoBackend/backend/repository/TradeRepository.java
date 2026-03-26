package com.cryptoBackend.backend.repository;

import com.cryptoBackend.backend.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, String> {
    List<Trade> findTop50ByOrderByTradeTimeDesc();

    @Query("SELECT t FROM Trade t WHERE t.buyOrder.user.id = :userId OR t.sellOrder.user.id = :userId ORDER BY t.tradeTime DESC")
    List<Trade> findUserTrades(@Param("userId") String userId);
}