package com.arnaldsouza.libraryapi.services;

import com.arnaldsouza.libraryapi.dto.AuthResponse;
import com.arnaldsouza.libraryapi.dto.LoginRequest;
import com.arnaldsouza.libraryapi.dto.RegisterRequest;
import com.arnaldsouza.libraryapi.entity.Role;
import com.arnaldsouza.libraryapi.entity.User;
import com.arnaldsouza.libraryapi.repository.UserRepository;
import com.arnaldsouza.libraryapi.security.JwtService;
import com.arnaldsouza.libraryapi.exception.UsernameAlreadyExistsException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                savedUser.getUsername(), savedUser.getRole().name());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameAlreadyExistsException(request.username()));

        String token = jwtService.generateToken(
                user.getUsername(), user.getRole().name());
        return new AuthResponse(token);
    }
}