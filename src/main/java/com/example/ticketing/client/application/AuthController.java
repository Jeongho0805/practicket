package com.example.ticketing.auth.application;

import com.example.ticketing.auth.dto.SessionObject;
import com.example.ticketing.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<SessionObject> getSessionObject(HttpServletRequest request) {
        SessionObject sessionObject = authService.getSessionObject(request);
        return ResponseEntity.ok(sessionObject);
    }

    @PostMapping("/token")
    public ResponseEntity<?> createToken(HttpServletRequest request) {
        TokenResponse token = authService.createToken(request);
        return ResponseEntity.ok(token);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSession(HttpServletRequest request) {
        authService.deleteSession(request);
        return ResponseEntity.ok().build();
    }
}
