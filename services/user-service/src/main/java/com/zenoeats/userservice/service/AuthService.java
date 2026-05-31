package com.zenoeats.userservice.service;

import com.zenoeats.userservice.dto.AuthResponse;
import com.zenoeats.userservice.dto.LoginRequest;
import com.zenoeats.userservice.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
