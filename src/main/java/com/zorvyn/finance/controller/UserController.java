package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.UserResponse;
import com.zorvyn.finance.dto.UserUpdateRequest;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody User user) {

        // Check if any user exists
        if (userService.isFirstUser()) {
            if (user.getRole() != com.zorvyn.finance.entity.Role.ADMIN) {
                throw new IllegalArgumentException("First user must have ADMIN role");
            }
            User saved = userService.createUser(user);
            return new ResponseEntity<>(toResponse(saved), HttpStatus.CREATED);
        }

        // After bootstrap → only ADMIN allowed
        userService.assertAdmin();

        User saved = userService.createUser(user);
        return new ResponseEntity<>(toResponse(saved), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAllUsers()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable String id) {
        return toResponse(userService.getUserById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable String id,
                               @Valid @RequestBody UserUpdateRequest request) {
        return toResponse(userService.updateUser(id, request));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}