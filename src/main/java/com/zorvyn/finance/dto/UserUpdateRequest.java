package com.zorvyn.finance.dto;

import com.zorvyn.finance.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private Role role;
    private Boolean active;

    @Size(min = 2, max = 100)
    private String name;

    @Email
    private String email;
}