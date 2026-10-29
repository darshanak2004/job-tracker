package com.jobtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.LoginRequest;
import com.jobtracker.dto.RegisterRequest;
import com.jobtracker.entity.Role;
import com.jobtracker.entity.User;
import com.jobtracker.exception.DuplicateResourceException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.security.AuthUser;
import com.jobtracker.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    void register_hashesPassword_normalisesEmail_andReturnsToken() {
        when(userRepository.existsByEmail("ann@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("jwt-token");

        AuthResponse res = authService.register(
                new RegisterRequest("  Ann ", "  ANN@test.com ", "secret123", Role.CANDIDATE));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("HASHED"); // never the raw password
        assertThat(saved.getValue().getEmail()).isEqualTo("ann@test.com");
        assertThat(saved.getValue().getName()).isEqualTo("Ann");
        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.user().id()).isEqualTo(7L);
        assertThat(res.user().role()).isEqualTo(Role.CANDIDATE);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("ann@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ann", "ann@test.com", "secret123", Role.RECRUITER)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsTokenAndUser() {
        User user = TestData.user(3L, Role.RECRUITER);
        AuthUser principal = AuthUser.from(user);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(principal)).thenReturn("jwt");

        AuthResponse res = authService.login(new LoginRequest(" USER3@test.com ", "pw"));

        assertThat(res.token()).isEqualTo("jwt");
        assertThat(res.user().email()).isEqualTo("user3@test.com");
    }

    @Test
    void login_badCredentials_propagates() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_userVanishedAfterAuthentication_throwsNotFound() {
        AuthUser principal = AuthUser.from(TestData.user(3L, Role.CANDIDATE));
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("user3@test.com", "pw")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void me_found() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(TestData.user(5L, Role.CANDIDATE)));

        assertThat(authService.me(5L).email()).isEqualTo("user5@test.com");
    }

    @Test
    void me_notFound() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
