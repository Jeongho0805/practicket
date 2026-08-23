package com.practicket.practice.application;

import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.practice.domain.PeriodType;
import com.practicket.practice.domain.PracticeType;
import com.practicket.practice.dto.PracticeCheckpointRequest;
import com.practicket.practice.dto.PracticeCompleteResponse;
import com.practicket.practice.dto.PracticeMyRecordsResponse;
import com.practicket.practice.dto.PracticeMyRankResponse;
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

    @PostMapping("/checkpoint")
    public ResponseEntity<Void> checkpoint(
            @Auth ClientInfo clientInfo,
            @Valid @RequestBody PracticeCheckpointRequest request
    ) {
        practiceService.checkpoint(clientInfo, request.getSessionId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complete")
    public ResponseEntity<PracticeCompleteResponse> complete(
            @Auth ClientInfo clientInfo,
            @Valid @RequestBody PracticeResultRequest request
    ) {
        return ResponseEntity.ok(practiceService.complete(clientInfo, request));
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

    @GetMapping("/my-rank")
    public ResponseEntity<PracticeMyRankResponse> getMyRank(
            @Auth ClientInfo clientInfo,
            @RequestParam @NotNull PracticeType type,
            @RequestParam(defaultValue = "MONTHLY") PeriodType period
    ) {
        return ResponseEntity.ok(practiceService.getMyRank(clientInfo, type, period));
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
