package com.practicket.community.admin;

import com.practicket.community.admin.dto.BanDuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 신고함·글댓글 관리 두 화면이 공유하는 처리 엔드포인트.
 * 화면마다 검색어·페이지 상태가 달라서 되돌아갈 주소(redirectTo)를 폼이 직접 들고 온다.
 */
@Slf4j
@Controller
@RequestMapping("/admin-hoya/community")
@RequiredArgsConstructor
public class AdminCommunityModerationController {

    private static final String DEFAULT_REDIRECT = "/admin-hoya/community/reports";

    private final AdminCommunityService adminCommunityService;

    @PostMapping("/posts/{id}/blind")
    public String blindPost(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                             RedirectAttributes ra) {
        return run(() -> adminCommunityService.blindPost(id), redirectTo, ra);
    }

    @PostMapping("/posts/{id}/unblind")
    public String unblindPost(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                               RedirectAttributes ra) {
        return run(() -> adminCommunityService.unblindPost(id), redirectTo, ra);
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                              RedirectAttributes ra) {
        return run(() -> adminCommunityService.deletePost(id), redirectTo, ra);
    }

    @PostMapping("/comments/{id}/blind")
    public String blindComment(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                                RedirectAttributes ra) {
        return run(() -> adminCommunityService.blindComment(id), redirectTo, ra);
    }

    @PostMapping("/comments/{id}/unblind")
    public String unblindComment(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                                  RedirectAttributes ra) {
        return run(() -> adminCommunityService.unblindComment(id), redirectTo, ra);
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                                 RedirectAttributes ra) {
        return run(() -> adminCommunityService.deleteComment(id), redirectTo, ra);
    }

    @PostMapping("/clients/{id}/ban")
    public String banClient(@PathVariable Long id,
                             @RequestParam BanDuration duration,
                             @RequestParam(required = false) String reason,
                             @RequestParam(required = false) String redirectTo,
                             RedirectAttributes ra) {
        return run(() -> adminCommunityService.banClient(id, duration, blankToNull(reason)), redirectTo, ra);
    }

    @PostMapping("/clients/{id}/unban")
    public String unbanClient(@PathVariable Long id, @RequestParam(required = false) String redirectTo,
                               RedirectAttributes ra) {
        return run(() -> adminCommunityService.unbanClient(id), redirectTo, ra);
    }

    private String run(Runnable action, String redirectTo, RedirectAttributes ra) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            log.warn("커뮤니티 모더레이션 처리 실패: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + safeRedirect(redirectTo);
    }

    /** redirectTo 는 폼이 들고 온 값이라, 엉뚱한 곳으로 리다이렉트되지 않게 어드민 커뮤니티 경로인지만 확인한다. */
    private String safeRedirect(String redirectTo) {
        if (redirectTo != null && redirectTo.startsWith("/admin-hoya/community")) {
            return redirectTo;
        }
        return DEFAULT_REDIRECT;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
