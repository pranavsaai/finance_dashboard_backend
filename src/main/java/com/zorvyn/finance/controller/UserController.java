package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.UserUpdateRequest;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        return new ResponseEntity<>(userService.createUser(user), HttpStatus.CREATED);
    }

    @GetMapping
    public List<User> getAll() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @PatchMapping("/{id}")
    public User update(@PathVariable String id,
                       @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }
}
