package com.example.ticketing.auth;

import com.example.ticketing.auth.dto.LoginRequestDto;
import com.example.ticketing.auth.dto.SessionObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final static String SESSION_KEY = "auth";

    @GetMapping
    public ResponseEntity<SessionObject> getSessionValues(HttpSession session) {
        SessionObject sessionObject = (SessionObject) session.getAttribute(SESSION_KEY);
        if (sessionObject == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(sessionObject);
    }

    @PostMapping
    public ResponseEntity<?> createSession(@RequestBody LoginRequestDto requestDto, HttpServletRequest request, HttpSession session) {
        String ip = authService.recordIp(request);
        session.setAttribute(SESSION_KEY, new SessionObject(ip, requestDto.getName()));
        return ResponseEntity.ok().build();
    }

    @PatchMapping
    public ResponseEntity<?> updateSession(@RequestBody LoginRequestDto requestDto, HttpSession session) {
        SessionObject sessionObject = (SessionObject) session.getAttribute(SESSION_KEY);
        if (sessionObject == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        sessionObject.setName(requestDto.getName());
        session.setAttribute(SESSION_KEY, sessionObject);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSession(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
}
