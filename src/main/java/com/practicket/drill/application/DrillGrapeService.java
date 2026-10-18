package com.practicket.drill.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.ValidateException;
import com.practicket.drill.domain.DrillGrapeResult;
import com.practicket.drill.domain.DrillGrapeResultRepository;
import com.practicket.drill.domain.DrillSeatForm;
import com.practicket.drill.dto.DrillGrapeResultRequest;
import com.practicket.drill.dto.DrillGrapeResultRequest.Round;
import com.practicket.drill.dto.DrillSeatRecordResponse;
import com.practicket.drill.dto.DrillSeatRecordResponse.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DrillGrapeService {

    private static final List<Integer> SEATS = List.of(4, 5, 6, 7, 8);

    private final DrillGrapeResultRepository drillGrapeResultRepository;
    private final ObjectMapper objectMapper;

    public DrillSeatRecordResponse createResult(ClientInfo clientInfo, DrillGrapeResultRequest request) {
        List<Round> rounds = request.getRounds();
        validate(rounds);

        int totalMs = rounds.stream().mapToInt(Round::getElapsedMs).sum();
        int seatCount = rounds.stream().mapToInt(Round::getSeats).sum();
        drillGrapeResultRepository.save(DrillGrapeResult.builder()
                .clientId(clientInfo.getClientId())
                .form(request.getForm())
                .averageMs((int) Math.round(totalMs / (double) seatCount))
                .totalMs(totalMs)
                .seatCount(seatCount)
                .misclicks(rounds.stream().mapToInt(Round::getMisclicks).sum())
                .rounds(toJson(rounds))
                .build());
        return getRecord(clientInfo, request.getForm());
    }

    @Transactional(readOnly = true)
    public DrillSeatRecordResponse getRecord(ClientInfo clientInfo, DrillSeatForm form) {
        Long clientId = clientInfo.getClientId();
        List<DrillGrapeResult> latestFirst = drillGrapeResultRepository.findTop10ByClientIdAndFormOrderByIdDesc(clientId, form);
        if (latestFirst.isEmpty()) {
            return DrillSeatRecordResponse.empty();
        }
        return DrillSeatRecordResponse.of(
                drillGrapeResultRepository.countByClientIdAndForm(clientId, form),
                item(drillGrapeResultRepository.findFirstByClientIdAndFormOrderByAverageMsAscIdAsc(clientId, form).orElseThrow()),
                drillGrapeResultRepository.findFirstByClientIdAndFormAndMisclicksOrderByAverageMsAscIdAsc(clientId, form, 0).map(this::item),
                latestFirst.stream().map(this::item).toList());
    }

    private Item item(DrillGrapeResult r) {
        return new Item(r.getAverageMs(), r.getMisclicks(), null, r.getCreatedAt());
    }

    private void validate(List<Round> rounds) {
        for (int i = 0; i < rounds.size(); i++) {
            Round round = rounds.get(i);
            if (!SEATS.get(i).equals(round.getSeats()) || round.getSoldClicks() > round.getMisclicks()) {
                throw new ValidateException(ErrorCode.PARAMETER_IS_NOT_VALID);
            }
        }
    }

    private String toJson(List<Round> rounds) {
        try {
            return objectMapper.writeValueAsString(rounds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("포도알 라운드 기록을 쓰지 못했습니다", e);
        }
    }
}
