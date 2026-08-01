package com.practicket.community.application;

import com.practicket.community.domain.repository.PostTagRepository;
import com.practicket.community.dto.PopularTagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 모든 목록 페이지에 뜨는 줄이라 요청마다 GROUP BY 하지 않는다. 매시간 스케줄러가 계산해 Redis 에 넣는다 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularTagService {

    private static final String CACHE_KEY = "community:popular-tags";
    private static final String DELIMITER = ",";
    /** 캐시 한 칸에서 태그 이름과 사용 수를 잇는 문자 */
    private static final String COUNT_SEPARATOR = ":";

    /** 갱신 주기(1시간)의 두 배 — 스케줄러가 한 번 걸러도 줄이 비지 않는다 */
    private static final long CACHE_TTL_HOURS = 2L;

    private static final int DISPLAY_COUNT = 8;
    private static final int RECENT_DAYS = 30;
    private static final long MIN_USE_COUNT = 3L;

    /** 오픈 직후엔 집계될 태그가 없어 줄이 빈다. 씨앗을 앞에 고정해 칸을 채운다 */
    private static final List<String> SEED_TAGS = List.of("꿀팁", "후기", "잡담");

    private final PostTagRepository postTagRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /** 캐시가 없으면 그 자리에서 한 번 계산한다 */
    public List<PopularTagResponse> getPopularTags() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                return parse(cached);
            }
        } catch (Exception e) {
            log.warn("인기 태그 캐시 조회 실패. 집계로 대신한다.", e);
            return calculate();
        }

        return refresh();
    }

    /** 계산해서 캐시를 덮어쓴다 */
    public List<PopularTagResponse> refresh() {
        List<PopularTagResponse> tags = calculate();
        try {
            stringRedisTemplate.opsForValue().set(CACHE_KEY, serialize(tags), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // 캐시에 못 넣어도 이번 응답은 정상이다
            log.warn("인기 태그 캐시 저장 실패", e);
        }
        return tags;
    }

    private List<PopularTagResponse> calculate() {
        // 씨앗이 먼저. 아직 안 쓰인 태그는 사용 수 0 이고 화면이 숫자를 감춘다
        Map<String, Long> counts = new LinkedHashMap<>();
        SEED_TAGS.forEach(tag -> counts.put(tag, 0L));

        postTagRepository.findPopularTags(LocalDateTime.now().minusDays(RECENT_DAYS), MIN_USE_COUNT)
                .forEach(row -> counts.put(row.getTag(), row.getUseCount()));

        return counts.entrySet().stream()
                .limit(DISPLAY_COUNT)
                .map(entry -> new PopularTagResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String serialize(List<PopularTagResponse> tags) {
        return tags.stream()
                .map(tag -> tag.tag() + COUNT_SEPARATOR + tag.useCount())
                .collect(Collectors.joining(DELIMITER));
    }

    private List<PopularTagResponse> parse(String cached) {
        if (cached.isBlank()) {
            return List.of();
        }

        return Arrays.stream(cached.split(DELIMITER))
                .map(entry -> {
                    int at = entry.lastIndexOf(COUNT_SEPARATOR);
                    // 형식이 깨진 값 때문에 게시판이 막히면 안 된다
                    return at < 0
                            ? new PopularTagResponse(entry, 0L)
                            : new PopularTagResponse(entry.substring(0, at), parseCount(entry.substring(at + 1)));
                })
                .toList();
    }

    private long parseCount(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
