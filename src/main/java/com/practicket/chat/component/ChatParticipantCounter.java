package com.practicket.chat.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 파드별 채팅 연결 수를 Redis 해시에 모아 전체 인원을 센다.
 * <p>
 * 값은 "연결수:기록시각" 이고 {@link #STALE_AFTER} 를 넘긴 항목은 죽은 파드로 보고 버린다.
 * INCR/DECR 로 누적하지 않는 이유는 파드가 비정상 종료하면 그 몫이 영원히 남기 때문이다.
 * 키 하나에 모아 두므로 조회가 HGETALL 한 번이면 끝난다(파드별 키 + SCAN 은 키스페이스를 훑는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatParticipantCounter {

    private static final String COUNT_KEY = "chat:participants";
    private static final Duration STALE_AFTER = Duration.ofSeconds(15);
    private static final long HEARTBEAT_MS = 5_000;

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic chatParticipantTopic;

    private final String instanceId = UUID.randomUUID().toString();

    /** Redis 가 답을 못 줄 때 내려줄 값. 전체는 몰라도 자기 파드 인원은 보여준다. */
    private volatile int localCount = 0;

    /** 연결이 늘거나 줄면 자기 몫을 갱신하고 모든 파드에 알린다 */
    public void report(int count) {
        localCount = count;
        write(count);
        try {
            stringRedisTemplate.convertAndSend(chatParticipantTopic.getTopic(), instanceId);
        } catch (Exception e) {
            log.warn("참여자 수 변경 알림 실패", e);
        }
    }

    /**
     * 연결 수가 한동안 그대로여도 기록 시각은 계속 밀어줘야 살아있는 파드로 인정된다.
     * 파드마다 자기 몫을 써야 하므로 ShedLock 을 걸지 않는다.
     */
    @Scheduled(fixedRate = HEARTBEAT_MS)
    public void heartbeat() {
        write(localCount);
    }

    // 종료 시 자기 몫을 지우지는 않는다. 스프링이 Redis 연결을 빈 소멸보다 먼저 닫아
    // @PreDestroy 에서는 이미 쓸 수 없기 때문이다. 정상 종료든 비정상 종료든
    // STALE_AFTER 가 지나면 어차피 버려지므로 결과는 같다.

    public int total() {
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(COUNT_KEY);
            long now = System.currentTimeMillis();
            long staleBefore = now - STALE_AFTER.toMillis();

            int sum = 0;
            List<Object> stale = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String[] parts = String.valueOf(entry.getValue()).split(":");
                if (parts.length != 2) {
                    stale.add(entry.getKey());
                    continue;
                }
                if (Long.parseLong(parts[1]) < staleBefore) {
                    stale.add(entry.getKey());
                    continue;
                }
                sum += Integer.parseInt(parts[0]);
            }
            if (!stale.isEmpty()) {
                stringRedisTemplate.opsForHash().delete(COUNT_KEY, stale.toArray());
            }
            return sum;
        } catch (Exception e) {
            log.warn("참여자 수 집계 실패 — 자기 파드 인원으로 대체한다", e);
            return localCount;
        }
    }

    private void write(int count) {
        try {
            stringRedisTemplate.opsForHash()
                    .put(COUNT_KEY, instanceId, count + ":" + System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("참여자 수 기록 실패", e);
        }
    }
}
