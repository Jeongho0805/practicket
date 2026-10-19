package com.practicket.drill.application;

import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.drill.domain.DrillSeatForm;
import com.practicket.drill.dto.DrillCancelResultRequest;
import com.practicket.drill.dto.DrillGrapeResultRequest;
import com.practicket.drill.dto.DrillSeatRecordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drill")
public class DrillSeatController {

    private final DrillGrapeService drillGrapeService;
    private final DrillCancelService drillCancelService;

    @PostMapping("/grape/results")
    public ResponseEntity<DrillSeatRecordResponse> createGrapeResult(@Auth ClientInfo clientInfo,
                                                                     @Valid @RequestBody DrillGrapeResultRequest request) {
        return ResponseEntity.ok(drillGrapeService.createResult(clientInfo, request));
    }

    @GetMapping("/grape/record")
    public ResponseEntity<DrillSeatRecordResponse> getGrapeRecord(@Auth ClientInfo clientInfo,
                                                                  @RequestParam DrillSeatForm form) {
        return ResponseEntity.ok(drillGrapeService.getRecord(clientInfo, form));
    }

    @PostMapping("/cancel/results")
    public ResponseEntity<DrillSeatRecordResponse> createCancelResult(@Auth ClientInfo clientInfo,
                                                                      @Valid @RequestBody DrillCancelResultRequest request) {
        return ResponseEntity.ok(drillCancelService.createResult(clientInfo, request));
    }

    @GetMapping("/cancel/record")
    public ResponseEntity<DrillSeatRecordResponse> getCancelRecord(@Auth ClientInfo clientInfo,
                                                                   @RequestParam DrillSeatForm form) {
        return ResponseEntity.ok(drillCancelService.getRecord(clientInfo, form));
    }
}
