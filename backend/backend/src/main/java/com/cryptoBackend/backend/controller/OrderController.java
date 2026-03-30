package com.cryptoBackend.backend.controller;

import com.cryptoBackend.backend.dto.OrderRequest;
import com.cryptoBackend.backend.dto.OrderResponse;
import com.cryptoBackend.backend.model.Order;
import com.cryptoBackend.backend.model.User;
import com.cryptoBackend.backend.service.OrderService;
import com.cryptoBackend.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        Order order = orderService.placeOrder(user, request);
        return ResponseEntity.ok(OrderResponse.fromOrder(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Order> orders = orderService.getUserOrders(user.getId());
        return ResponseEntity.ok(orders.stream()
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.ok().build();
    }


}