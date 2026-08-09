package com.practicket.practice.share;

import com.practicket.practice.domain.PracticeType;

/**
 * 공유 링크가 실어 나르는 결과. 서버에 저장하지 않고 쿼리 파라미터로만 오간다.
 * 값이 조작될 수 있지만 남의 기록을 바꾸는 게 아니라 자기 카드만 바뀌므로 그대로 둔다.
 */
public record ShareResult(
        PracticeType type,
        int totalMs,
        int reactionMs,
        int queueMs,
        int seatMs,
        int queueInitialRank,
        Integer percentile
) {
    private static final int MAX_MS = 60 * 60 * 1000;
    private static final int MAX_QUEUE_RANK = 9_999_999;

    public static ShareResult of(PracticeType type, int totalMs, int reactionMs, int queueMs,
                                 int seatMs, int queueInitialRank, Integer percentile) {
        return new ShareResult(
                type == null ? PracticeType.I_TICKET_OLD : type,
                clamp(totalMs, MAX_MS),
                clamp(reactionMs, MAX_MS),
                clamp(queueMs, MAX_MS),
                clamp(seatMs, MAX_MS),
                clamp(queueInitialRank, MAX_QUEUE_RANK),
                percentile == null ? null : Math.max(1, Math.min(100, percentile))
        );
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }

    public int segmentSum() {
        return reactionMs + queueMs + seatMs;
    }

    public String label() {
        return switch (type) {
            case N_TICKET -> "N-Ticket";
            case M_TICKET -> "M-Ticket";
            default -> "I-Ticket";
        };
    }

    /** 가장 오래 걸린 구간의 이름과 비중. 카드와 설명 문구가 같은 값을 쓴다. */
    public String slowestLabel() {
        int max = Math.max(reactionMs, Math.max(queueMs, seatMs));
        if (max == seatMs) return "좌석 선택";
        if (max == queueMs) return "대기열";
        return "반응";
    }

    public int slowestShare() {
        int sum = segmentSum();
        if (sum == 0) return 0;
        int max = Math.max(reactionMs, Math.max(queueMs, seatMs));
        return Math.round(max * 100f / sum);
    }

    public String totalSeconds() {
        return String.format("%.3f", totalMs / 1000f);
    }
}
