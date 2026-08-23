package com.practicket.community.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageBlockTest {

    @Test
    @DisplayName("페이지가 많아도 번호는 블록 크기만큼만 나온다 — 이게 이 클래스를 만든 이유다")
    void showsOnlyOneBlockNoMatterHowManyPages() {
        PageBlock block = PageBlock.of(0, 500);

        assertThat(block.pageNumbers()).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    @DisplayName("현재 페이지가 속한 블록을 그린다 — 27번 페이지(0-based)는 25~29 묶음이다")
    void picksBlockOfCurrentPage() {
        PageBlock block = PageBlock.of(27, 57);

        assertThat(block.pageNumbers()).containsExactly(25, 26, 27, 28, 29);
    }

    @Test
    @DisplayName("블록 안에서 페이지를 옮겨도 번호는 그대로다 — 숫자가 밀리지 않는 게 블록식의 이점이다")
    void keepsSameNumbersWithinBlock() {
        assertThat(PageBlock.of(25, 57).pageNumbers())
                .isEqualTo(PageBlock.of(29, 57).pageNumbers());
    }

    @Test
    @DisplayName("블록을 벗어나면 다음 묶음으로 바뀐다")
    void movesToNextBlockAtBoundary() {
        assertThat(PageBlock.of(30, 57).pageNumbers()).containsExactly(30, 31, 32, 33, 34);
    }

    @Test
    @DisplayName("마지막 블록은 남은 만큼만 그린다 — 57페이지면 55·56 두 개뿐이다")
    void lastBlockMayBeShorter() {
        PageBlock block = PageBlock.of(56, 57);

        assertThat(block.pageNumbers()).containsExactly(55, 56);
        assertThat(block.hasNextBlock()).isFalse();
        assertThat(block.last()).isTrue();
    }

    @Test
    @DisplayName("‹ 는 이전 블록의 마지막 페이지로, › 는 다음 블록의 첫 페이지로 간다")
    void arrowsJumpBetweenBlocks() {
        PageBlock block = PageBlock.of(27, 57);

        assertThat(block.prevBlockPage()).isEqualTo(24);
        assertThat(block.nextBlockPage()).isEqualTo(30);
        assertThat(block.hasPrevBlock()).isTrue();
        assertThat(block.hasNextBlock()).isTrue();
    }

    @Test
    @DisplayName("첫 블록에서는 ‹ 가 잠긴다")
    void locksPrevArrowOnFirstBlock() {
        PageBlock block = PageBlock.of(3, 57);

        assertThat(block.hasPrevBlock()).isFalse();
        // 첫 블록이어도 1페이지가 아니면 « 는 살아 있다 — 4페이지에서 1페이지로 갈 수 있어야 한다
        assertThat(block.first()).isFalse();
    }

    @Test
    @DisplayName("« » 는 첫·마지막 페이지를 가리킨다")
    void endButtonsPointToFirstAndLastPage() {
        PageBlock block = PageBlock.of(27, 57);

        assertThat(block.lastPage()).isEqualTo(56);
        assertThat(block.first()).isFalse();
        assertThat(block.last()).isFalse();
    }

    @Test
    @DisplayName("주소창에 범위 밖 페이지를 쳐도 마지막 블록으로 떨어진다 — 사용자가 직접 고칠 수 있는 곳이다")
    void clampsPageBeyondRange() {
        PageBlock block = PageBlock.of(9999, 57);

        assertThat(block.pageNumbers()).containsExactly(55, 56);
        assertThat(block.last()).isTrue();
    }

    @Test
    @DisplayName("음수 페이지도 첫 블록으로 떨어진다")
    void clampsNegativePage() {
        PageBlock block = PageBlock.of(-5, 57);

        assertThat(block.pageNumbers()).containsExactly(0, 1, 2, 3, 4);
        assertThat(block.first()).isTrue();
    }

    @Test
    @DisplayName("글이 하나도 없어도 터지지 않는다 — 1페이지짜리로 취급한다")
    void handlesEmptyResult() {
        PageBlock block = PageBlock.of(0, 0);

        assertThat(block.pageNumbers()).containsExactly(0);
        assertThat(block.first()).isTrue();
        assertThat(block.last()).isTrue();
        assertThat(block.hasPrevBlock()).isFalse();
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    @DisplayName("블록 크기가 0 이하면 거부한다")
    void rejectsNonPositiveBlockSize() {
        assertThatThrownBy(() -> PageBlock.of(0, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
