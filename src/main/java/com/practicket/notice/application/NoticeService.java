package com.practicket.notice.application;

import com.practicket.notice.domain.Notice;
import com.practicket.notice.domain.NoticeRepository;
import com.practicket.notice.domain.NoticeRow;
import com.practicket.notice.domain.NoticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 공개 화면(홈·/notice)이 쓰는 조회 전용 서비스. 쓰기는 {@link AdminNoticeService} 가 맡는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** 홈 공지 카드에 몇 건을 띄울지. landing.html 레이아웃이 5줄 기준이다. */
    private static final int HOME_SIZE = 5;

    /** 목록 한 배치 크기. 첫 화면도 무한스크롤 추가분도 같은 값을 쓴다. */
    public static final int LIST_SIZE = 15;

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final NoticeRepository noticeRepository;

    public List<Notice> getRecentForHome() {
        return noticeRepository
                .findByPublishedTrueOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, HOME_SIZE))
                .getContent();
    }

    /** type 이 null 이면 전체. 페이지 크기는 {@link #LIST_SIZE} 로 고정한다. */
    public Page<Notice> getPublishedPage(NoticeType type, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), LIST_SIZE);
        return type == null
                ? noticeRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(pageable)
                : noticeRepository.findByPublishedTrueAndTypeOrderByPinnedDescCreatedAtDesc(type, pageable);
    }

    /**
     * 목록 줄에 월 구분 헤더를 붙일지 서버가 계산한다.
     *
     * 무한스크롤은 배치가 나뉘어 도착하므로, 클라이언트가 직전 배치의 마지막 달(previousMonthKey)을
     * 같이 보내준다. 그래야 6월이 두 배치에 걸쳐 있을 때 "2026년 6월" 헤더가 두 번 찍히지 않는다.
     */
    public List<NoticeRow> toRows(List<Notice> notices, String previousMonthKey) {
        List<NoticeRow> rows = new ArrayList<>(notices.size());
        String prev = previousMonthKey;
        for (Notice notice : notices) {
            String key = notice.getCreatedAt().format(MONTH_KEY);
            rows.add(new NoticeRow(notice, key.equals(prev) ? null : toMonthLabel(key), key));
            prev = key;
        }
        return rows;
    }

    public long countAll() {
        return noticeRepository.countByPublishedTrue();
    }

    public long countByType(NoticeType type) {
        return noticeRepository.countByPublishedTrueAndType(type);
    }

    /**
     * 비공개 글은 없는 것으로 취급한다.
     * 호출부가 404 대신 목록으로 돌려보내도록 Optional 로 넘긴다.
     */
    public Optional<Notice> getPublished(Long id) {
        return noticeRepository.findByIdAndPublishedTrue(id);
    }

    /** "2026-06" → "2026년 6월" */
    private String toMonthLabel(String monthKey) {
        String[] parts = monthKey.split("-");
        return parts[0] + "년 " + Integer.parseInt(parts[1]) + "월";
    }
}
