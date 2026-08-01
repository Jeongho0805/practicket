package com.practicket.ad.admin;

import com.practicket.ad.application.AdminBannerController;
import com.practicket.ad.application.AdminDashboardController;
import com.practicket.ad.application.AdminSlotController;
import com.practicket.ad.domain.AdSlotRepository;
import com.practicket.ad.domain.BannerRepository;
import com.practicket.community.admin.AdminCommunityPostController;
import com.practicket.community.admin.AdminCommunityReportController;
import com.practicket.community.domain.repository.PostReportRepository;
import com.practicket.notice.application.AdminNoticeController;
import com.practicket.notice.domain.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 사이드바 배지 숫자를 모든 어드민 화면에 공급한다. 컨트롤러마다 같은 카운트를 담지 않으려는 목적.
 * 대상 테이블이 작아 매 요청 count 쿼리로 충분하다.
 */
@ControllerAdvice(assignableTypes = {
        AdminDashboardController.class,
        AdminBannerController.class,
        AdminSlotController.class,
        AdminNoticeController.class,
        AdminCommunityReportController.class,
        AdminCommunityPostController.class
})
@RequiredArgsConstructor
public class AdminNavAdvice {

    private final BannerRepository bannerRepository;
    private final AdSlotRepository adSlotRepository;
    private final NoticeRepository noticeRepository;
    private final PostReportRepository postReportRepository;

    @ModelAttribute("navBannerCount")
    public long navBannerCount() {
        return bannerRepository.count();
    }

    @ModelAttribute("navSlotCount")
    public long navSlotCount() {
        return adSlotRepository.count();
    }

    @ModelAttribute("navNoticeCount")
    public long navNoticeCount() {
        return noticeRepository.count();
    }

    /**
     * 신고함 배지 = 신고가 들어온 "대상" 수(글+댓글 합)이지 신고 행 수가 아니다.
     * findReportedTargets 가 대상별로 묶어주는 쿼리라 그 결과의 총 개수를 그대로 쓴다
     * (1페이지만 조회해도 Page.getTotalElements()는 전체 대상 수를 돌려준다).
     */
    @ModelAttribute("navReportCount")
    public long navReportCount() {
        return postReportRepository.findReportedTargets(PageRequest.of(0, 1)).getTotalElements();
    }
}
