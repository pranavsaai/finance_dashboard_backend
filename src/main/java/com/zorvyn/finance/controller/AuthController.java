package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.AuthResponse;
import com.zorvyn.finance.dto.LoginRequest;
import com.zorvyn.finance.dto.RefreshRequest;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.security.JwtUtil;
import com.zorvyn.finance.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {

        User user = userService.login(request.getEmail(), request.getPassword());

        String accessToken = jwtUtil.generateToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {

        String refreshToken = request.getRefreshToken();

        // validateRefreshTokenAndExtractUserId() validates the token signature, expiry,
        // and type claim in a single parse — previously isRefreshToken() and extractUserId()
        // each parsed the token independently, doubling the work.
        String userId;
        try {
            userId = jwtUtil.validateRefreshTokenAndExtractUserId(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        User user = userService.getUserForRefresh(userId);

        String newAccessToken = jwtUtil.generateToken(userId, user.getRole().name());

        return new AuthResponse(newAccessToken, refreshToken);
    }
}