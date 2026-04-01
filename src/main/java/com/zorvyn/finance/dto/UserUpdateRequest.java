package com.zorvyn.finance.dto;

import com.zorvyn.finance.entity.Role;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private Role role;
    private Boolean active;
}
