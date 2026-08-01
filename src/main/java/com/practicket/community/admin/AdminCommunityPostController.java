package com.practicket.community.admin;

import com.practicket.community.admin.dto.AdminCommentView;
import com.practicket.community.admin.dto.AdminPostView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 지워진 글·댓글도 함께 보여준다. 이미 삭제된 항목은 상태만 보여주고 관리 버튼은 숨긴다 */
@Controller
@RequestMapping("/admin-hoya/community/posts")
@RequiredArgsConstructor
public class AdminCommunityPostController {

    private static final int PAGE_SIZE = 20;

    private final AdminCommunityService adminCommunityService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "POST") String type,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);

        if ("COMMENT".equalsIgnoreCase(type)) {
            Page<AdminCommentView> comments = adminCommunityService.searchComments(keyword, PageRequest.of(page, PAGE_SIZE));
            model.addAttribute("comments", comments);
        } else {
            Page<AdminPostView> posts = adminCommunityService.searchPosts(keyword, PageRequest.of(page, PAGE_SIZE));
            model.addAttribute("posts", posts);
        }
        return "admin/community/post-list";
    }
}
