package com.practicket.drill.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.TreeMap;

/**
 * 정답 글자별로 나온 횟수, 실수한 자리 수, 대신 친 영문 글자를 센다. 키 순서를 고정하려고 TreeMap 을 쓴다.
 * 한 자리에서 여러 번 틀려도 실수한 자리는 1이고, 대신 친 글자에는 친 만큼 남는다.
 */
public class CaptchaLetterStats {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<TreeMap<String, Letter>> TYPE = new TypeReference<>() {
    };

    private final TreeMap<String, Letter> letters;

    private CaptchaLetterStats(TreeMap<String, Letter> letters) {
        this.letters = letters;
    }

    public static CaptchaLetterStats empty() {
        return new CaptchaLetterStats(new TreeMap<>());
    }

    public static CaptchaLetterStats fromJson(String json) {
        try {
            return new CaptchaLetterStats(MAPPER.readValue(json, TYPE));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("보안문자 누적 오타를 읽지 못했습니다", e);
        }
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(letters);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("보안문자 누적 오타를 쓰지 못했습니다", e);
        }
    }

    public void addSeen(char answer) {
        letter(answer).seen++;
    }

    public void addMissedSlot(char answer) {
        letter(answer).slots++;
    }

    public void addTyped(char answer, char typed) {
        letter(answer).typed.merge(String.valueOf(typed), 1, Integer::sum);
    }

    public Map<String, Letter> asMap() {
        return letters;
    }

    private Letter letter(char answer) {
        return letters.computeIfAbsent(String.valueOf(answer), k -> new Letter());
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Letter {
        public int seen;
        public int slots;
        public TreeMap<String, Integer> typed = new TreeMap<>();

        public int miss() {
            return typed.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
}
