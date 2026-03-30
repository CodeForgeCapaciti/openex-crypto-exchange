package com.cryptoBackend.backend.controller;

import com.cryptoBackend.backend.dto.AuthRequest;
import com.cryptoBackend.backend.dto.AuthResponse;
import com.cryptoBackend.backend.model.User;
import com.cryptoBackend.backend.service.UserService;
import com.cryptoBackend.backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        User user = userService.register(request.getEmail(), request.getPassword(), request.getUsername());
        userService.initializeWallets(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getUsername()));
    }


}