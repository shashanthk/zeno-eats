package com.zenoeats.userservice.service.impl;

import com.zenoeats.userservice.dto.AuthResponse;
import com.zenoeats.userservice.dto.LoginRequest;
import com.zenoeats.userservice.dto.RegisterRequest;
import com.zenoeats.userservice.entity.User;
import com.zenoeats.userservice.exception.EmailAlreadyExistsException;
import com.zenoeats.userservice.repository.UserRepository;
import com.zenoeats.userservice.security.JwtService;
import com.zenoeats.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // authenticate() throws BadCredentialsException if credentials are wrong —
        // GlobalExceptionHandler maps that to 401 with a vague message
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
            .token(jwtService.generateToken(user))
            .expiresIn(jwtService.getExpirationMs() / 1000)
            .build();
    }
}
