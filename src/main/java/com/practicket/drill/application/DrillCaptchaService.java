package com.practicket.drill.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.ValidateException;
import com.practicket.drill.domain.CaptchaLetterStats;
import com.practicket.drill.domain.DrillCaptchaResult;
import com.practicket.drill.domain.DrillCaptchaResultRepository;
import com.practicket.drill.domain.DrillCaptchaStat;
import com.practicket.drill.domain.DrillCaptchaStatRepository;
import com.practicket.drill.dto.DrillCaptchaResultRequest;
import com.practicket.drill.dto.DrillCaptchaResultRequest.Problem;
import com.practicket.drill.dto.DrillCaptchaResultRequest.Typo;
import com.practicket.drill.dto.DrillCaptchaTypoStatsResponse;
import com.practicket.drill.dto.DrillRecordResponse;
import com.practicket.drill.dto.DrillRecordResponse.CaptchaItem;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class DrillCaptchaService {

    private static final int TIME_LIMIT_MS = 60_000;

    private final DrillCaptchaResultRepository drillCaptchaResultRepository;
    private final DrillCaptchaStatRepository drillCaptchaStatRepository;
    private final DrillCaptchaStatCreator drillCaptchaStatCreator;
    private final ObjectMapper objectMapper;

    public DrillRecordResponse<CaptchaItem> createResult(ClientInfo clientInfo, DrillCaptchaResultRequest request) {
        List<Problem> problems = request.getProblems();
        validate(problems);
        Long clientId = clientInfo.getClientId();

        int totalMs = problems.stream().mapToInt(Problem::getElapsedMs).sum();
        drillCaptchaResultRepository.save(DrillCaptchaResult.builder()
                .clientId(clientId)
                .averageMs(Math.round(totalMs / (float) problems.size()))
                .problems(toJson(problems))
                .build());

        accumulate(clientId, problems);
        return getRecord(clientInfo);
    }

    @Transactional(readOnly = true)
    public DrillRecordResponse<CaptchaItem> getRecord(ClientInfo clientInfo) {
        Long clientId = clientInfo.getClientId();
        List<DrillCaptchaResultRepository.Row> latestFirst = drillCaptchaResultRepository.findRecent(clientId);
        if (latestFirst.isEmpty()) {
            return DrillRecordResponse.empty();
        }

        List<CaptchaItem> recent = new ArrayList<>(latestFirst.stream()
                .map(r -> new CaptchaItem(r.averageMs(), r.createdAt()))
                .toList());
        Collections.reverse(recent);

        DrillCaptchaResultRepository.Row best = drillCaptchaResultRepository.findBest(clientId).orElseThrow();
        long count = drillCaptchaResultRepository.countByClientId(clientId);
        return new DrillRecordResponse<>(count, best.averageMs(), best.createdAt(), recent);
    }

    @Transactional(readOnly = true)
    public DrillCaptchaTypoStatsResponse getTypoStats(ClientInfo clientInfo) {
        return drillCaptchaStatRepository.findByClientId(clientInfo.getClientId())
                .map(stat -> new DrillCaptchaTypoStatsResponse(
                        stat.letterStats().asMap().entrySet().stream()
                                .map(e -> new DrillCaptchaTypoStatsResponse.Letter(
                                        e.getKey(),
                                        e.getValue().seen,
                                        e.getValue().slots,
                                        e.getValue().miss(),
                                        e.getValue().typed.entrySet().stream()
                                                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                                .map(t -> new DrillCaptchaTypoStatsResponse.Typed(t.getKey(), t.getValue()))
                                                .toList()))
                                .toList(),
                        stat.getImeCount()))
                .orElse(new DrillCaptchaTypoStatsResponse(List.of(), 0));
    }

    private void validate(List<Problem> problems) {
        int totalMs = 0;
        for (Problem problem : problems) {
            totalMs += problem.getElapsedMs();
            for (Typo typo : problem.getTypos()) {
                if (typo.getTyped().charAt(0) == problem.getAnswer().charAt(typo.getPosition())) {
                    throw new ValidateException(ErrorCode.PARAMETER_IS_NOT_VALID);
                }
            }
        }
        if (totalMs > TIME_LIMIT_MS) {
            throw new ValidateException(ErrorCode.PARAMETER_IS_NOT_VALID);
        }
    }

    private void accumulate(Long clientId, List<Problem> problems) {
        // 잠금 없는 확인을 먼저 한다. 없는 키를 FOR UPDATE 로 읽으면 갭 락이 걸려 새 트랜잭션의 INSERT 와 서로 기다린다.
        if (!drillCaptchaStatRepository.existsByClientId(clientId)) {
            try {
                drillCaptchaStatCreator.createIfAbsent(clientId);
            } catch (DataIntegrityViolationException ignored) {
                // 같은 사람의 다른 요청이 먼저 만들었다
            }
        }
        DrillCaptchaStat stat = drillCaptchaStatRepository.findByClientIdForUpdate(clientId).orElseThrow();

        CaptchaLetterStats letters = stat.letterStats();
        int imeAdded = 0;
        for (Problem problem : problems) {
            String answer = problem.getAnswer();
            for (char c : answer.toCharArray()) {
                letters.addSeen(c);
            }
            Set<Integer> missedSlots = new HashSet<>();
            for (Typo typo : problem.getTypos()) {
                if (DrillCaptchaResultRequest.IME.equals(typo.getTyped())) {
                    imeAdded++;
                } else {
                    letters.addTyped(answer.charAt(typo.getPosition()), typo.getTyped().charAt(0));
                    missedSlots.add(typo.getPosition());
                }
            }
            missedSlots.forEach(position -> letters.addMissedSlot(answer.charAt(position)));
        }
        stat.update(letters, imeAdded);
    }

    private String toJson(List<Problem> problems) {
        try {
            return objectMapper.writeValueAsString(problems);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("보안문자 문제 기록을 쓰지 못했습니다", e);
        }
    }
}
