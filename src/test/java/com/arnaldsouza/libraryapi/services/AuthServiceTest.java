package com.arnaldsouza.libraryapi.services;

import com.arnaldsouza.libraryapi.dto.AuthResponse;
import com.arnaldsouza.libraryapi.dto.LoginRequest;
import com.arnaldsouza.libraryapi.dto.RegisterRequest;
import com.arnaldsouza.libraryapi.entity.Role;
import com.arnaldsouza.libraryapi.entity.User;
import com.arnaldsouza.libraryapi.exception.UsernameAlreadyExistsException;
import com.arnaldsouza.libraryapi.repository.UserRepository;
import com.arnaldsouza.libraryapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("arnald");
        existingUser.setPassword("$2a$10$hashedpassword");
        existingUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("register should hash the password before saving")
    void registerShouldHashPassword() {
        RegisterRequest request = new RegisterRequest("newuser", "plainpassword");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token123");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("plainpassword");
    }

    @Test
    @DisplayName("register should always assign the USER role")
    void registerShouldAssignUserRole() {
        RegisterRequest request = new RegisterRequest("newuser", "plainpassword");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token123");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("register should return a token")
    void registerShouldReturnToken() {
        RegisterRequest request = new RegisterRequest("newuser", "plainpassword");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("newuser", "USER")).thenReturn("token123");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("token123");
    }

    @Test
    @DisplayName("register should reject a duplicated username")
    void registerShouldRejectDuplicatedUsername() {
        RegisterRequest request = new RegisterRequest("arnald", "plainpassword");

        when(userRepository.findByUsername("arnald")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("login should return a token for valid credentials")
    void loginShouldReturnToken() {
        LoginRequest request = new LoginRequest("arnald", "supersecret123");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("arnald")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken("arnald", "USER")).thenReturn("token456");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("token456");
    }

    @Test
    @DisplayName("login should propagate bad credentials and not issue a token")
    void loginShouldFailOnBadCredentials() {
        LoginRequest request = new LoginRequest("arnald", "wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }
}