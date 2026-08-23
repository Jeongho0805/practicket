package com.practicket.community.application;

import com.practicket.community.domain.repository.PostTagRepository;
import com.practicket.community.dto.PopularTagResponse;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PopularTagServiceTest {

    @InjectMocks
    private PopularTagService popularTagService;

    @Mock
    private PostTagRepository postTagRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("캐시가 있으면 집계하지 않는다 — 매 요청 GROUP BY 를 막는 게 목적이다")
    void readsFromCacheWithoutAggregating() {
        // given
        when(valueOperations.get(anyString())).thenReturn("꿀팁:0,후기:12,세븐틴:9");

        // when
        List<String> tags = tagNamesOf(popularTagService.getPopularTags());

        // then
        assertThat(tags).containsExactly("꿀팁", "후기", "세븐틴");
        verify(postTagRepository, never()).findPopularTags(any(), anyLong());
    }

    @Test
    @DisplayName("캐시에 담긴 사용 수까지 되살린다")
    void restoresUseCountFromCache() {
        // given
        when(valueOperations.get(anyString())).thenReturn("꿀팁:0,후기:12");

        // when
        List<PopularTagResponse> tags = popularTagService.getPopularTags();

        // then — 아직 안 쓰인 씨앗은 0 이라 화면에서 숫자를 감춘다
        assertThat(tags.get(0).hasUseCount()).isFalse();
        assertThat(tags.get(1).useCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("씨앗 태그가 앞에 온다 — 오픈 직후 줄이 비지 않게")
    void putsSeedTagsFirst() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(postTagRepository.findPopularTags(any(), anyLong())).thenReturn(List.of(tagRow("세븐틴", 9L)));

        // when
        List<String> tags = tagNamesOf(popularTagService.getPopularTags());

        // then
        assertThat(tags).containsExactly("꿀팁", "후기", "잡담", "세븐틴");
    }

    @Test
    @DisplayName("씨앗과 겹치는 인기 태그는 자리를 두 번 차지하지 않는다")
    void doesNotDuplicateSeedTags() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(postTagRepository.findPopularTags(any(), anyLong())).thenReturn(List.of(tagRow("후기", 12L)));

        // when
        List<String> tags = tagNamesOf(popularTagService.getPopularTags());

        // then
        assertThat(tags).containsExactly("꿀팁", "후기", "잡담");
    }

    @Test
    @DisplayName("여덟 개까지만 내보낸다")
    void limitsToEightTags() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(postTagRepository.findPopularTags(any(), anyLong())).thenReturn(
                IntStream.range(0, 20).mapToObj(i -> tagRow("태그" + i, 10L - i)).toList());

        // when
        List<String> tags = tagNamesOf(popularTagService.getPopularTags());

        // then
        assertThat(tags).hasSize(8);
        assertThat(tags.subList(0, 3)).containsExactly("꿀팁", "후기", "잡담");
    }

    @Test
    @DisplayName("Redis 가 죽어도 집계 결과로 화면을 채운다")
    void survivesRedisFailure() {
        // given
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(postTagRepository.findPopularTags(any(), anyLong())).thenReturn(List.of(tagRow("세븐틴", 5L)));

        // when
        List<String> tags = tagNamesOf(popularTagService.getPopularTags());

        // then
        assertThat(tags).containsExactly("꿀팁", "후기", "잡담", "세븐틴");
    }

    private List<String> tagNamesOf(List<PopularTagResponse> tags) {
        return tags.stream().map(PopularTagResponse::tag).toList();
    }

    private PostTagRepository.TagUseCount tagRow(String tag, long useCount) {
        return new PostTagRepository.TagUseCount() {
            @Override
            public String getTag() {
                return tag;
            }

            @Override
            public Long getUseCount() {
                return useCount;
            }
        };
    }
}
