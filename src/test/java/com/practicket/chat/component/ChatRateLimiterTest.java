package com.practicket.chat.component;

import com.practicket.common.component.RedisRateLimiter;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.GlobalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 채팅만 다른 리미터와 다르게, 넘쳤다고 바로 막지 않고 경고를 쌓았다가 임시 밴으로 올라간다.
 * 그 단계 전환이 이 테스트가 보는 것이다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRateLimiterTest {

    private static final String TOKEN = "client-token-abc";
    private static final String RATE_KEY = "chat:rate:" + TOKEN;
    private static final String WARN_KEY = "chat:warn:" + TOKEN;
    private static final String TEMPBAN_KEY = "chat:tempban:" + TOKEN;

    @InjectMocks
    private ChatRateLimiter chatRateLimiter;

    @Mock
    private RedisRateLimiter rateLimiter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("임시 밴 중이면 세어보지도 않고 막는다")
    void rejectsWhileTempBanned() {
        when(stringRedisTemplate.hasKey(TEMPBAN_KEY)).thenReturn(true);

        assertThatThrownBy(() -> chatRateLimiter.validate(TOKEN))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_TEMP_BANNED);

        verify(rateLimiter, never()).countWithin(anyString(), anyLong());
    }

    @Test
    @DisplayName("문턱 이내면 통과한다")
    void passesWithinLimit() {
        when(stringRedisTemplate.hasKey(TEMPBAN_KEY)).thenReturn(false);
        when(rateLimiter.countWithin(RATE_KEY, 1L)).thenReturn(2L);

        assertThatCode(() -> chatRateLimiter.validate(TOKEN)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("문턱을 넘으면 경고를 쌓고 막는다 — 아직 임시 밴은 아니다")
    void warnsWhenExceeded() {
        when(stringRedisTemplate.hasKey(TEMPBAN_KEY)).thenReturn(false);
        when(rateLimiter.countWithin(RATE_KEY, 1L)).thenReturn(3L);
        when(rateLimiter.countWithin(WARN_KEY, 600L)).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> chatRateLimiter.validate(TOKEN))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED);

        verify(valueOperations, never()).set(eq(TEMPBAN_KEY), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("경고가 5회 쌓이면 임시 밴으로 올라가고 경고 카운터는 지운다")
    void tempBansAfterFifthWarning() {
        when(stringRedisTemplate.hasKey(TEMPBAN_KEY)).thenReturn(false);
        when(rateLimiter.countWithin(RATE_KEY, 1L)).thenReturn(3L);
        when(rateLimiter.countWithin(WARN_KEY, 600L)).thenReturn(5L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> chatRateLimiter.validate(TOKEN))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_TEMP_BANNED);

        verify(valueOperations).set(TEMPBAN_KEY, "1", 600L, TimeUnit.SECONDS);
        // 경고 카운터를 안 지우면 밴이 풀린 직후 한 번만 넘겨도 바로 다시 밴된다
        verify(stringRedisTemplate).delete(WARN_KEY);
    }
}
