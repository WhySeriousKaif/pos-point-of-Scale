package com.molla.mapper;

import com.molla.model.User;
import com.molla.payload.dto.UserDto;

public class UserMapper {

    public static UserDto toDto(User newUser) {
        UserDto userDto = new UserDto();

        userDto.setFullName(newUser.getFullName());
        userDto.setEmail(newUser.getEmail());
        userDto.setPhone(newUser.getPhone());
        userDto.setRole(newUser.getRole());
        userDto.setCreatedAt(newUser.getCreatedAt());
        userDto.setUpdatedAt(newUser.getUpdatedAt());
        userDto.setLastLoginAt(newUser.getLastLoginAt());
        return  userDto;

    }
}
