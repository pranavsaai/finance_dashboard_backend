package com.zorvyn.finance.dto;

import com.zorvyn.finance.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Input DTO for user creation.
 *
 * Accepts only the fields a caller is allowed to supply: name, email, password, and role.
 * Fields like id, active, and createdAt are intentionally excluded — they are set
 * internally by the service layer and must not be accepted from API consumers.
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required (VIEWER, ANALYST, ADMIN)")
    private Role role;
}