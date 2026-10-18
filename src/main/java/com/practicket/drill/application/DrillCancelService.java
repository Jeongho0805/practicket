package com.practicket.drill.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.ValidateException;
import com.practicket.drill.domain.DrillCancelResult;
import com.practicket.drill.domain.DrillCancelResultRepository;
import com.practicket.drill.domain.DrillSeatForm;
import com.practicket.drill.dto.DrillCancelResultRequest;
import com.practicket.drill.dto.DrillCancelResultRequest.Round;
import com.practicket.drill.dto.DrillSeatRecordResponse;
import com.practicket.drill.dto.DrillSeatRecordResponse.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DrillCancelService {

    private static final int GENERAL_MIN_MS = 100;

    private final DrillCancelResultRepository drillCancelResultRepository;
    private final ObjectMapper objectMapper;

    public DrillSeatRecordResponse createResult(ClientInfo clientInfo, DrillCancelResultRequest request) {
        DrillSeatForm form = request.getForm();
        List<Round> rounds = request.getRounds();
        rounds.forEach(round -> validate(form, round));

        int totalMs = rounds.stream().mapToInt(Round::getElapsedMs).sum();
        drillCancelResultRepository.save(DrillCancelResult.builder()
                .clientId(clientInfo.getClientId())
                .form(form)
                .averageMs((int) Math.round(totalMs / (double) rounds.size()))
                .totalMs(totalMs)
                .misclicks(rounds.stream().mapToInt(Round::getMisclicks).sum())
                .missedTickets(form == DrillSeatForm.GENERAL ? rounds.stream().mapToInt(Round::getMissed).sum() : null)
                .rounds(toJson(rounds))
                .build());
        return getRecord(clientInfo, form);
    }

    @Transactional(readOnly = true)
    public DrillSeatRecordResponse getRecord(ClientInfo clientInfo, DrillSeatForm form) {
        Long clientId = clientInfo.getClientId();
        List<DrillCancelResult> latestFirst = drillCancelResultRepository.findTop10ByClientIdAndFormOrderByIdDesc(clientId, form);
        if (latestFirst.isEmpty()) {
            return DrillSeatRecordResponse.empty();
        }
        Optional<DrillCancelResult> cleanBest = form == DrillSeatForm.GENERAL
                ? drillCancelResultRepository.findFirstByClientIdAndFormAndMisclicksAndMissedTicketsOrderByAverageMsAscIdAsc(clientId, form, 0, 0)
                : drillCancelResultRepository.findFirstByClientIdAndFormAndMisclicksOrderByAverageMsAscIdAsc(clientId, form, 0);
        return DrillSeatRecordResponse.of(
                drillCancelResultRepository.countByClientIdAndForm(clientId, form),
                item(drillCancelResultRepository.findFirstByClientIdAndFormOrderByAverageMsAscIdAsc(clientId, form).orElseThrow()),
                cleanBest.map(this::item),
                latestFirst.stream().map(this::item).toList());
    }

    private Item item(DrillCancelResult r) {
        return new Item(r.getAverageMs(), r.getMisclicks(), r.getMissedTickets(), r.getCreatedAt());
    }

    private void validate(DrillSeatForm form, Round round) {
        boolean valid = round.getSoldClicks() <= round.getMisclicks() && (form == DrillSeatForm.GENERAL
                ? round.getElapsedMs() >= GENERAL_MIN_MS && round.getDiscoverMs() + round.getSelectMs() < round.getElapsedMs()
                : round.getDiscoverMs() == 0 && round.getSelectMs().equals(round.getElapsedMs()) && round.getMissed() == 0);
        if (!valid) {
            throw new ValidateException(ErrorCode.PARAMETER_IS_NOT_VALID);
        }
    }

    private String toJson(List<Round> rounds) {
        try {
            return objectMapper.writeValueAsString(rounds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("취소표 라운드 기록을 쓰지 못했습니다", e);
        }
    }
}
