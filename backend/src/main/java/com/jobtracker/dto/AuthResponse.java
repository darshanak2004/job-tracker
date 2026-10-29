package com.jobtracker.dto;

public record AuthResponse(String token, UserResponse user) {
}
