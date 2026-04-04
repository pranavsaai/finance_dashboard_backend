package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.UserUpdateRequest;
import com.zorvyn.finance.entity.Role;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.AccessDeniedException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.AuthContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    /**
     * Validates credentials and returns the authenticated user.
     * Throws UnauthorizedException for invalid email, wrong password, or inactive account.
     */
    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        if (!encoder.matches(rawPassword, user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return user;
    }

    /**
     * Fetches a user by ID for token refresh.
     * Throws UnauthorizedException if not found.
     */
    public User getUserForRefresh(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        resolveCallerAsAdmin();
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        resolveCallerAsAdmin();
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Admin can update another user's role or activate/deactivate them.
     * Partial update: only fields that are non-null in the request are applied.
     */
    public User updateUser(String id, UserUpdateRequest request) {
        resolveCallerAsAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return userRepository.save(user);
    }

    /**
     * Resolves the calling user from AuthContext and asserts they are ADMIN.
     */
    private User resolveCallerAsAdmin() {
        User caller = resolveCaller();
        if (caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can manage users");
        }
        return caller;
    }

    public User resolveCaller() {
        String callerId = AuthContext.get();
        if (callerId == null || callerId.isBlank()) {
            throw new UnauthorizedException("Missing or invalid authentication token");
        }
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caller user not found"));
        if (!caller.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }
        return caller;
    }

    public boolean isFirstUser() {
        return userRepository.count() == 0;
    }

    public void assertAdmin() {
        User caller = resolveCaller();
        if (caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}