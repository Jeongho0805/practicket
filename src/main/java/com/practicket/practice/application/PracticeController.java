package com.practicket.practice.application;

import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeMyRecordsResponse;
import com.practicket.practice.dto.PracticeMyStatsResponse;
import com.practicket.practice.dto.PracticeRankResponse;
import com.practicket.practice.dto.PracticeResultRequest;
import com.practicket.practice.dto.PracticeStartResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/start")
    public ResponseEntity<PracticeStartResponse> start(
            @Auth ClientInfo clientInfo,
            @RequestParam @NotNull PracticeType type
    ) {
        PracticeStartResponse response = practiceService.start(clientInfo, type);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> complete(
            @Auth ClientInfo clientInfo,
            @Valid @RequestBody PracticeResultRequest request
    ) {
        practiceService.complete(clientInfo, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rank")
    public ResponseEntity<PracticeRankResponse> getRanking(
            @RequestParam @NotNull PracticeType type,
            @RequestParam @NotNull PeriodType period,
            @RequestParam(required = false) Integer cursorTotalDurationMs,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        PracticeRankResponse response = practiceService.getRanking(
                type, period, cursorTotalDurationMs, cursorId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-stats")
    public ResponseEntity<PracticeMyStatsResponse> getMyStats(
            @Auth ClientInfo clientInfo,
            @RequestParam @NotNull PracticeType type
    ) {
        return ResponseEntity.ok(practiceService.getMyStats(clientInfo, type));
    }

    @GetMapping("/my-records")
    public ResponseEntity<PracticeMyRecordsResponse> getMyRecords(
            @Auth ClientInfo clientInfo,
            @RequestParam @NotNull PracticeType type,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(practiceService.getMyRecords(clientInfo, type, cursorId, limit));
    }
}
