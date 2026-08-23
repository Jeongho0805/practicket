package com.practicket.notice.application;

import com.practicket.notice.domain.Notice;
import com.practicket.notice.domain.NoticeRepository;
import com.practicket.notice.domain.NoticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 어드민 공지 CRUD. 인증은 /admin-hoya/** 필터가 이미 통과시킨 것으로 가정한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<Notice> getAll() {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Notice get(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지입니다. id=" + id));
    }

    public void create(NoticeType type, String title, String content, boolean pinned, boolean published) {
        noticeRepository.save(Notice.builder()
                .type(type)
                .title(title.strip())
                .content(content.strip())
                .pinned(pinned)
                .published(published)
                .build());
    }

    public void update(Long id, NoticeType type, String title, String content, boolean pinned, boolean published) {
        get(id).update(type, title.strip(), content.strip(), pinned, published);
    }

    /** 운영자가 직접 쓰고 지우는 데이터라 보존 요구가 없다. 행을 실제로 지운다. */
    public void delete(Long id) {
        noticeRepository.delete(get(id));
    }

    public void togglePublished(Long id) {
        get(id).togglePublished();
    }
}
