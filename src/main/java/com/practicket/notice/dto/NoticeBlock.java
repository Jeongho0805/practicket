package com.practicket.notice.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 운영자가 쓴 순수 텍스트 본문을 화면 블록으로 나눈다.
 * "## " 로 시작하는 줄은 중제목, "·" 로 시작하는 줄은 목록으로 묶는다.
 * 문단은 놓인 자리에 따라 lead(첫 문단) · p · tail(목록 뒤 맺음말) 로 갈린다.
 */
public record NoticeBlock(String kind, String text, List<String> items) {

    public static final String LEAD = "lead";
    public static final String PARAGRAPH = "p";
    public static final String LIST = "ul";
    public static final String TAIL = "tail";
    public static final String HEADING = "h2";

    private static final String BULLET = "·";

    public static List<NoticeBlock> parse(String content) {
        List<NoticeBlock> blocks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return blocks;
        }

        List<String> paragraph = new ArrayList<>();
        List<String> bullets = new ArrayList<>();

        for (String raw : content.split("\\R")) {
            String line = raw.strip();

            if (line.startsWith("## ")) {
                flushParagraph(blocks, paragraph);
                flushList(blocks, bullets);
                blocks.add(new NoticeBlock(HEADING, line.substring(3).strip(), List.of()));
                continue;
            }

            if (line.startsWith(BULLET)) {
                flushParagraph(blocks, paragraph);
                bullets.add(line.substring(BULLET.length()).strip());
                continue;
            }

            flushList(blocks, bullets);

            if (line.isEmpty()) {
                flushParagraph(blocks, paragraph);
            } else {
                paragraph.add(line);
            }
        }

        flushParagraph(blocks, paragraph);
        flushList(blocks, bullets);
        // 중제목이 있는 공지는 각 절의 설명과 마지막 안내를 구분한다.
        if (!blocks.isEmpty() && blocks.stream().anyMatch(b -> HEADING.equals(b.kind()))) {
            int last = blocks.size() - 1;
            NoticeBlock block = blocks.get(last);
            if (PARAGRAPH.equals(block.kind()) && last > 0 && LIST.equals(blocks.get(last - 1).kind())) {
                blocks.set(last, new NoticeBlock(TAIL, block.text(), block.items()));
            }
        }
        return blocks;
    }

    private static void flushParagraph(List<NoticeBlock> blocks, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        blocks.add(new NoticeBlock(paragraphKind(blocks), String.join("\n", paragraph), List.of()));
        paragraph.clear();
    }

    private static void flushList(List<NoticeBlock> blocks, List<String> bullets) {
        if (bullets.isEmpty()) {
            return;
        }
        blocks.add(new NoticeBlock(LIST, null, List.copyOf(bullets)));
        bullets.clear();
    }

    private static String paragraphKind(List<NoticeBlock> blocks) {
        if (blocks.isEmpty()) {
            return LEAD;
        }
        if (blocks.stream().anyMatch(b -> HEADING.equals(b.kind()))) {
            return PARAGRAPH;
        }
        return blocks.stream().anyMatch(b -> LIST.equals(b.kind())) ? TAIL : PARAGRAPH;
    }
}
