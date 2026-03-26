package com.cryptoBackend.backend.repository;

import com.cryptoBackend.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByUserIdAndStatusIn(String userId, List<Order.OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status IN :statuses ORDER BY o.createdAt ASC")
    List<Order> findActiveOrdersBySymbol(@Param("symbol") String symbol,
                                         @Param("statuses") List<Order.OrderStatus> statuses);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") Order.OrderStatus status);
}