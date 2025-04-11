package com.example.ticketing.auth;

import com.example.ticketing.auth.dto.LoginRequestDto;
import com.example.ticketing.auth.dto.SessionObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<?> createSession(@Valid @RequestBody LoginRequestDto dto, HttpServletRequest request) {
        authService.createSession(request, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSession(HttpServletRequest request) {
        authService.deleteSession(request);
        return ResponseEntity.ok().build();
    }
}
