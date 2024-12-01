package com.example.ticketing.auth;

import com.example.ticketing.auth.dto.LoginRequestDto;
import com.example.ticketing.auth.dto.SessionObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    public ResponseEntity<?> createSession(@RequestBody LoginRequestDto requestDto, HttpServletRequest request) {
        authService.createSession(request, requestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSession(HttpServletRequest request) {
        authService.deleteSession(request);
        return ResponseEntity.ok().build();
    }
}
