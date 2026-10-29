package com.jobtracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.config.SecurityConfig;
import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.LoginRequest;
import com.jobtracker.dto.RegisterRequest;
import com.jobtracker.dto.UserResponse;
import com.jobtracker.entity.Role;
import com.jobtracker.security.CustomUserDetailsService;
import com.jobtracker.security.JwtService;
import com.jobtracker.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService userDetailsService;

    private static final String VALID_REGISTER = """
            {"name":"Ann","email":"ann@test.com","password":"secret123","role":"CANDIDATE"}""";

    @Test
    void register_validBody_returns201AndToken() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(
                new AuthResponse("tok", new UserResponse(1L, "Ann", "ann@test.com", Role.CANDIDATE)));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok"))
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"));
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        String bad = """
                {"name":"","email":"not-an-email","password":"short","role":null}""";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void register_unknownRole_returns400() throws Exception {
        String bad = """
                {"name":"Ann","email":"ann@test.com","password":"secret123","role":"ADMIN"}""";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ok() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new AuthResponse("tok", new UserResponse(1L, "Ann", "ann@test.com", Role.RECRUITER)));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ann@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ann@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void me_withoutToken_returns401Json() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_authenticated_returnsProfile() throws Exception {
        when(authService.me(2L)).thenReturn(new UserResponse(2L, "Cand", "cand@test.com", Role.CANDIDATE));

        mvc.perform(get("/api/auth/me").with(ControllerTestSupport.candidate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cand@test.com"));
    }
}
