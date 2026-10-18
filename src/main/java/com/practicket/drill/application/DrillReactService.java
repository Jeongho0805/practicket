package com.practicket.drill.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.ValidateException;
import com.practicket.drill.domain.DrillReactForm;
import com.practicket.drill.domain.DrillReactResult;
import com.practicket.drill.domain.DrillReactResultRepository;
import com.practicket.drill.dto.DrillReactResultRequest;
import com.practicket.drill.dto.DrillRecordResponse;
import com.practicket.drill.dto.DrillRecordResponse.ReactItem;
import com.practicket.drill.dto.DrillSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DrillReactService {

    private final DrillReactResultRepository drillReactResultRepository;

    public DrillRecordResponse<ReactItem> createResult(ClientInfo clientInfo, DrillReactResultRequest request) {
        if (request.getForm().hasLatency() != (request.getLatencyMs() != null)) {
            throw new ValidateException(ErrorCode.PARAMETER_IS_NOT_VALID);
        }
        drillReactResultRepository.save(DrillReactResult.builder()
                .clientId(clientInfo.getClientId())
                .form(request.getForm())
                .reactionMs(request.getReactionMs())
                .latencyMs(request.getLatencyMs())
                .build());
        return getRecord(clientInfo, request.getForm());
    }

    @Transactional(readOnly = true)
    public DrillRecordResponse<ReactItem> getRecord(ClientInfo clientInfo, DrillReactForm form) {
        Long clientId = clientInfo.getClientId();
        List<DrillReactResult> latestFirst = drillReactResultRepository.findTop10ByClientIdAndFormOrderByIdDesc(clientId, form);
        if (latestFirst.isEmpty()) {
            return DrillRecordResponse.empty();
        }

        List<ReactItem> recent = new ArrayList<>(latestFirst.stream()
                .map(r -> new ReactItem(r.getReactionMs(), r.getLatencyMs(), r.getCreatedAt()))
                .toList());
        Collections.reverse(recent);

        DrillReactResult best = drillReactResultRepository.findFirstByClientIdAndFormOrderByReactionMsAscIdAsc(clientId, form)
                .orElseThrow();
        long count = drillReactResultRepository.countByClientIdAndForm(clientId, form);
        return new DrillRecordResponse<>(count, best.getReactionMs(), best.getCreatedAt(), recent);
    }

    @Transactional(readOnly = true)
    public DrillSummaryResponse.React getSummary(ClientInfo clientInfo) {
        Long clientId = clientInfo.getClientId();
        return new DrillSummaryResponse.React(
                drillReactResultRepository.countByClientId(clientId),
                drillReactResultRepository.findBests(clientId));
    }
}
