package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.UserCreateRequest;
import com.zorvyn.finance.dto.UserUpdateRequest;
import com.zorvyn.finance.entity.Role;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.exception.AccessDeniedException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.AuthContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

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

    // also checks isActive() — a valid refresh token doesn't mean active account
    public User getUserForRefresh(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        return user;
    }

    public User getCurrentUser() {
        return resolveCaller();
    }

    public User createUser(UserCreateRequest request) {
        // app-level check for a clean error message; unique index is the real safety net
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // catches the rare concurrent-save race the check above can't prevent
            throw new IllegalArgumentException("A user with this email already exists");
        }
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

    public User updateUser(String id, UserUpdateRequest request) {
        User caller = resolveCallerAsAdmin();

        // changing own role or deactivating self = instant lockout, no recovery
        if (caller.getId().equals(id)) {
            if (request.getRole() != null && request.getRole() != caller.getRole()) {
                throw new IllegalArgumentException("Admin cannot change their own role");
            }
            if (request.getActive() != null && !request.getActive()) {
                throw new IllegalArgumentException("Admin cannot deactivate their own account");
            }
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getActive() != null) user.setActive(request.getActive());
        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) {
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("A user with this email already exists");
            }
            user.setEmail(request.getEmail());
        }

        return userRepository.save(user);
    }

    private User resolveCallerAsAdmin() {
        User caller = resolveCaller();
        if (caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can manage users");
        }
        return caller;
    }

    // live DB fetch — catches deactivated users that stale JWTs would miss
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