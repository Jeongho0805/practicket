package com.practicket.practice.infra.persistence;

public record PracticeRankEntry(Long id, String nickname, int totalDurationMs,
                                int reactionTimeMs, int queueWaitMs, int captchaMs, int seatSelectionMs) {
}
