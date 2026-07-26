package com.practicket.view;

import com.practicket.community.application.PostService;
import com.practicket.community.component.PostContentRenderer;
import com.practicket.community.dto.PostListResponse;
import com.practicket.community.dto.PostResponse;
import com.practicket.community.dto.PostSearchCondition;
import com.practicket.ticket.application.TicketQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final TicketQueueService ticketQueueService;
    private final PostService postService;

    @GetMapping("/")
    public String main(Model model) {
        return "main";
    }

    @GetMapping("/rank")
    public String RankPage(Model model) {
        return "rank";
    }

    @GetMapping("/reservation")
    public String reservationPage(@RequestParam(required = false) String token, Model model) {
        if (!ticketQueueService.isValidReservationToken(token)) {
            return "redirect:/";
        }
        return "reservation";
    }

    @GetMapping("/security")
    public String securityLetterPage(Model model) {
        return "security";
    }

    @GetMapping("/blog")
    public String blogList(Model model) {
        return "blog";
    }

    @GetMapping("/blog/{id}")
    public String blogContents(@PathVariable("id") String id, Model model) {
        return "blog/" + id;
    }

    @GetMapping("/art")
    public String artGallery(Model model) {
        return "art/gallery";
    }

    @GetMapping("/art/create")
    public String artCreate(Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("artId", null);
        return "art/create";
    }

    @GetMapping("/art/edit/{id}")
    public String artEdit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("isEdit", true);
        model.addAttribute("artId", id);
        return "art/create";
    }

    @GetMapping("/art/{id}")
    public String artDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("artId", id);
        return "art/detail";
    }
    /**
     * 커뮤니티 목록·상세는 **서버에서 렌더링한다.**
     * 검색 유입이 이 기능의 목적 중 하나인데(1항), JS 로 그리면 크롤러가 본문을 못 본다.
     * 페이징도 링크로 두어 2페이지 이후가 색인에서 빠지지 않게 한다(Q9).
     */
    @GetMapping("/community")
    public String communityList(
            @ModelAttribute PostSearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {
        Page<PostListResponse> posts = postService.search(condition, pageable);
        model.addAttribute("posts", posts);
        model.addAttribute("keyword", condition.getKeyword());
        return "community/list";
    }

    @GetMapping("/community/write")
    public String communityWrite(Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("postId", null);
        return "community/write";
    }

    @GetMapping("/community/edit/{id}")
    public String communityEdit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("isEdit", true);
        model.addAttribute("postId", id);
        return "community/write";
    }

    /**
     * 토큰은 브라우저 localStorage 에 있어 서버 렌더링 시점에는 알 수 없다.
     * 그래서 본문은 서버가 그리고, 수정·삭제 버튼 노출만 JS 가 결정한다.
     */
    @GetMapping("/community/{id}")
    public String communityDetail(@PathVariable("id") Long id, Model model) {
        PostResponse post = postService.get(id, null);
        model.addAttribute("post", post);
        model.addAttribute("renderedContent", PostContentRenderer.render(post.getContent()));
        return "community/detail";
    }

    @GetMapping("/practice")
    public String practiceList(Model model) {
        return "practice/list";
    }

    @GetMapping("/practice/i-ticket")
    public String practiceITicket(Model model) {
        return "practice/i_ticket";
    }

    @GetMapping("/practice/i-ticket/intro")
    public String practiceITicketIntro(Model model) {
        return "practice/i_ticket_intro";
    }
}
