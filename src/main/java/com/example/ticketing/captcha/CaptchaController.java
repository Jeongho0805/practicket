package com.example.ticketing.captcha;

import com.example.ticketing.captcha.dto.CaptchaCreateRequest;
import com.example.ticketing.captcha.dto.CaptchaResultStatistic;
import com.example.ticketing.common.auth.User;
import com.example.ticketing.common.auth.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping
    public ResponseEntity<CaptchaResultStatistic> getStatistic(@User UserInfo userInfo) {
        CaptchaResultStatistic response = captchaService.getStatistic(userInfo);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createCaptchaResult(@User UserInfo userInfo, @Valid @RequestBody CaptchaCreateRequest requestDto) {
        captchaService.createResult(userInfo, requestDto);
        return ResponseEntity.ok().build();
    }
}
