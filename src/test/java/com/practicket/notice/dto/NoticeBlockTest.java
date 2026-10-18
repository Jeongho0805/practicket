package com.practicket.notice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeBlockTest {
    @Test
    void 문단_안의_줄바꿈을_유지한다() {
        var blocks = NoticeBlock.parse("첫 문장\n두 번째 문장\n\n## 중제목\n설명");
        assertThat(blocks.get(0).text()).isEqualTo("첫 문장\n두 번째 문장");
        assertThat(blocks.get(0).kind()).isEqualTo("lead");
    }

    @Test
    void 기존_공지의_문단과_맺음말을_유지한다() {
        var blocks = NoticeBlock.parse("소개\n\n추가 설명\n\n· 첫 항목\n· 둘째 항목\n\n마지막 안내");
        assertThat(blocks).extracting(NoticeBlock::kind).containsExactly("lead", "p", "ul", "tail");
        assertThat(blocks.get(2).items()).containsExactly("첫 항목", "둘째 항목");
    }

    @Test
    void 중제목_아래_설명은_맺음말로_처리하지_않는다() {
        var blocks = NoticeBlock.parse("소개\n\n## 반응속도\n설명\n· 항목\n## 보안문자\n다음 설명\n· 다음 항목\n\n마지막 안내");
        assertThat(blocks).extracting(NoticeBlock::kind)
                .containsExactly("lead", "h2", "p", "ul", "h2", "p", "ul", "tail");
        assertThat(blocks.get(4).text()).isEqualTo("보안문자");
        assertThat(blocks.get(5).text()).isEqualTo("다음 설명");
    }
}
