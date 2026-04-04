package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.AuthResponse;
import com.zorvyn.finance.dto.LoginRequest;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;

    @PostMapping("/login")
    public AuthResponse login(@jakarta.validation.Valid @RequestBody LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
                
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken);
    }


    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody String refreshToken) {

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String userId = jwtUtil.extractUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newAccessToken = jwtUtil.generateToken(userId, user.getRole().name());

        return new AuthResponse(newAccessToken, refreshToken);
    }
}