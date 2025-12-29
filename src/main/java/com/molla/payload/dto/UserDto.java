package com.molla.payload.dto;

import java.time.LocalDateTime;

import com.molla.domain.UserRole;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private String password;

    private Long branchId;
    private Long storeId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
