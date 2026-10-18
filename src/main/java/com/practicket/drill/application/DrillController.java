package com.practicket.drill.application;

import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.drill.domain.DrillReactForm;
import com.practicket.drill.dto.DrillCaptchaResultRequest;
import com.practicket.drill.dto.DrillCaptchaTypoStatsResponse;
import com.practicket.drill.dto.DrillReactResultRequest;
import com.practicket.drill.dto.DrillRecordResponse;
import com.practicket.drill.dto.DrillRecordResponse.CaptchaItem;
import com.practicket.drill.dto.DrillRecordResponse.ReactItem;
import com.practicket.drill.dto.DrillSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drill")
public class DrillController {

    private final DrillReactService drillReactService;
    private final DrillCaptchaService drillCaptchaService;

    @GetMapping("/summary")
    public ResponseEntity<DrillSummaryResponse> getSummary(@Auth ClientInfo clientInfo) {
        return ResponseEntity.ok(new DrillSummaryResponse(
                drillReactService.getSummary(clientInfo),
                drillCaptchaService.getSummary(clientInfo)));
    }

    @PostMapping("/react/results")
    public ResponseEntity<DrillRecordResponse<ReactItem>> createReactResult(@Auth ClientInfo clientInfo,
                                                                            @Valid @RequestBody DrillReactResultRequest request) {
        return ResponseEntity.ok(drillReactService.createResult(clientInfo, request));
    }

    @GetMapping("/react/record")
    public ResponseEntity<DrillRecordResponse<ReactItem>> getReactRecord(@Auth ClientInfo clientInfo,
                                                                         @RequestParam DrillReactForm form) {
        return ResponseEntity.ok(drillReactService.getRecord(clientInfo, form));
    }

    @PostMapping("/captcha/results")
    public ResponseEntity<DrillRecordResponse<CaptchaItem>> createCaptchaResult(@Auth ClientInfo clientInfo,
                                                                                @Valid @RequestBody DrillCaptchaResultRequest request) {
        return ResponseEntity.ok(drillCaptchaService.createResult(clientInfo, request));
    }

    @GetMapping("/captcha/record")
    public ResponseEntity<DrillRecordResponse<CaptchaItem>> getCaptchaRecord(@Auth ClientInfo clientInfo) {
        return ResponseEntity.ok(drillCaptchaService.getRecord(clientInfo));
    }

    @GetMapping("/captcha/typo-stats")
    public ResponseEntity<DrillCaptchaTypoStatsResponse> getCaptchaTypoStats(@Auth ClientInfo clientInfo) {
        return ResponseEntity.ok(drillCaptchaService.getTypoStats(clientInfo));
    }
}
