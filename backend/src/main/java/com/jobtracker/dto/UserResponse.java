package com.jobtracker.dto;

import com.jobtracker.entity.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}
