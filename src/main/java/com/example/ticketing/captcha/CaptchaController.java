//package com.example.ticketing.captcha;
//
//import com.example.ticketing.captcha.dto.CaptchaCreateRequest;
//import com.example.ticketing.captcha.dto.CaptchaResultStatistic;
//import com.example.ticketing.common.auth.Auth;
//import com.example.ticketing.common.auth.ClientInfo;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("api/captcha")
//public class CaptchaController {
//
//    private final CaptchaService captchaService;
//
//    @GetMapping
//    public ResponseEntity<CaptchaResultStatistic> getStatistic(@Auth ClientInfo userInfo) {
//        CaptchaResultStatistic response = captchaService.getStatistic(userInfo);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping
//    public ResponseEntity<?> createCaptchaResult(@Auth ClientInfo userInfo, @Valid @RequestBody CaptchaCreateRequest requestDto) {
//        captchaService.createResult(userInfo, requestDto);
//        return ResponseEntity.ok().build();
//    }
//}
