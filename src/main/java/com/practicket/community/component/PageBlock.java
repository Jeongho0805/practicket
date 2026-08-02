package com.practicket.community.component;

import java.util.List;
import java.util.stream.IntStream;

/** 게시판식 블록 페이징. 페이지 번호는 전부 0-based 라 {@code Pageable} 과 그대로 맞물린다. */
public record PageBlock(
        List<Integer> pageNumbers,
        int prevBlockPage,
        int nextBlockPage,
        int lastPage,
        boolean hasPrevBlock,
        boolean hasNextBlock,
        boolean first,
        boolean last
) {

    /** 모바일 375px 한 줄에 들어가는 최대치. mine.js 도 같은 값을 쓴다 */
    public static final int BLOCK_SIZE = 5;

    public static PageBlock of(int currentPage, int totalPages) {
        return of(currentPage, totalPages, BLOCK_SIZE);
    }

    public static PageBlock of(int currentPage, int totalPages, int blockSize) {
        if (blockSize < 1) {
            throw new IllegalArgumentException("블록 크기는 1 이상이어야 한다: " + blockSize);
        }

        int pageCount = Math.max(totalPages, 1);
        // ?page=9999 처럼 범위 밖 주소로 들어와도 마지막 블록을 그린다
        int current = Math.min(Math.max(currentPage, 0), pageCount - 1);

        int blockStart = (current / blockSize) * blockSize;
        int blockEnd = Math.min(blockStart + blockSize - 1, pageCount - 1);

        return new PageBlock(
                IntStream.rangeClosed(blockStart, blockEnd).boxed().toList(),
                blockStart - 1,
                blockEnd + 1,
                pageCount - 1,
                blockStart > 0,
                blockEnd < pageCount - 1,
                current == 0,
                current == pageCount - 1
        );
    }
}
