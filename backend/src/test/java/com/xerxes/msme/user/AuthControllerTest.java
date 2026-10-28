package com.xerxes.msme.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.dto.AuthResponse;
import com.xerxes.msme.user.dto.LoginRequest;
import com.xerxes.msme.user.dto.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private AuthContext authContext;

    @Test
    void register_validRequest_returns201WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test Traders", null, "27", null, null, null, null, null, "Ravi", "ravi@example.com",
                "SecurePass123");
        AuthResponse response = new AuthResponse(
                "jwt-token", UUID.randomUUID(), UUID.randomUUID(), "ravi@example.com", Role.OWNER, "Test Traders");
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void register_blankBusinessName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", null, "27", null, null, null, null, null, "Ravi", "ravi@example.com", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_malformedStateCode_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test Traders", null, "MH", null, null, null, null, null, "Ravi", "ravi@example.com",
                "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emailWithoutDomainExtension_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test Traders", null, "27", null, null, null, null, null, "Ravi", "ravi@test", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_tooShortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test Traders", null, "27", null, null, null, null, null, "Ravi", "ravi@example.com", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("ravi@example.com", "SecurePass123");
        AuthResponse response = new AuthResponse(
                "jwt-token", UUID.randomUUID(), UUID.randomUUID(), "ravi@example.com", Role.OWNER, "Test Traders");
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_blankEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest("", "SecurePass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_revokesTheCurrentRequestsToken() throws Exception {
        UUID tokenId = UUID.randomUUID();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), UUID.randomUUID(), "ravi@example.com", Role.OWNER, tokenId);
        when(authContext.current()).thenReturn(principal);

        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());

        verify(authService).logout(tokenId);
    }
}
