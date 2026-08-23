package com.practicket.notice.application;

import com.practicket.notice.domain.Notice;
import com.practicket.notice.domain.NoticeRepository;
import com.practicket.notice.domain.NoticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 조회 전용. 쓰기는 {@link AdminNoticeService} 가 맡는다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** landing.html 레이아웃이 5줄 기준이다 */
    private static final int HOME_SIZE = 5;

    /** 첫 화면과 무한스크롤 추가분이 같은 값을 쓴다 */
    public static final int LIST_SIZE = 15;


    private final NoticeRepository noticeRepository;

    public List<Notice> getRecentForHome() {
        return noticeRepository
                .findByPublishedTrueOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, HOME_SIZE))
                .getContent();
    }

    /** type 이 null 이면 전체 */
    public Page<Notice> getPublishedPage(NoticeType type, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), LIST_SIZE);
        return type == null
                ? noticeRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(pageable)
                : noticeRepository.findByPublishedTrueAndTypeOrderByPinnedDescCreatedAtDesc(type, pageable);
    }


    public long countAll() {
        return noticeRepository.countByPublishedTrue();
    }

    public long countByType(NoticeType type) {
        return noticeRepository.countByPublishedTrueAndType(type);
    }

    /** 비공개 글은 없는 것으로 취급한다 */
    public Optional<Notice> getPublished(Long id) {
        return noticeRepository.findByIdAndPublishedTrue(id);
    }

}
