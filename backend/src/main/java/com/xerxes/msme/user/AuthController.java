package com.xerxes.msme.user;

import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.user.dto.AuthResponse;
import com.xerxes.msme.user.dto.LoginRequest;
import com.xerxes.msme.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;

    public AuthController(AuthService authService, AuthContext authContext) {
        this.authService = authService;
        this.authContext = authContext;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(authContext.current().tokenId());
        return ResponseEntity.noContent().build();
    }
}
