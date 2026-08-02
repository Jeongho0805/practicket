package com.practicket.view;

import com.practicket.community.application.PostCommentService;
import com.practicket.community.application.PopularTagService;
import com.practicket.community.application.PostService;
import com.practicket.community.component.CommunityListLinks;
import com.practicket.community.component.PageBlock;
import com.practicket.community.component.PostContentRenderer;
import com.practicket.community.component.TagNormalizer;
import com.practicket.community.domain.repository.PostQueryCondition;
import com.practicket.community.dto.PostListResponse;
import com.practicket.community.dto.PostResponse;
import com.practicket.community.dto.PostSearchCondition;
import com.practicket.notice.application.NoticeService;
import com.practicket.notice.domain.Notice;
import com.practicket.notice.domain.NoticeType;
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

    /** 목록·댓글 DTO 와 같은 문구다(Q7). 완전히 지우지 않고 자리를 남긴다. */

    private final TicketQueueService ticketQueueService;
    private final PostService postService;
    private final PostCommentService postCommentService;
    private final PopularTagService popularTagService;
    private final NoticeService noticeService;

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("notices", noticeService.getRecentForHome());
        return "landing";
    }

    /**
     * 공지 목록. 껍데기와 필터 탭만 내려주고 목록은 JS 가 /api/notices 로 채운다 —
     * 랭킹(practice/list)과 같은 방식이다.
     * 필터는 JS 토글이 아니라 링크(?type=)로 둔다. 주소만으로 상태가 복원된다.
     */
    @GetMapping("/notice")
    public String noticeList(@RequestParam(required = false) NoticeType type, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("countAll", noticeService.countAll());
        model.addAttribute("countNotice", noticeService.countByType(NoticeType.NOTICE));
        model.addAttribute("countFix", noticeService.countByType(NoticeType.FIX));
        return "notice/list";
    }

    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable("id") Long id, Model model) {
        Notice notice = noticeService.getPublished(id).orElse(null);
        // 비공개·삭제된 공지는 404 대신 목록으로 돌려보낸다. 링크가 오래 남는 성격의 글이라
        // 없어진 공지를 눌렀을 때 에러 화면보다 목록이 낫다.
        if (notice == null) {
            return "redirect:/notice";
        }
        model.addAttribute("notice", notice);
        return "notice/detail";
    }

    @GetMapping("/ticketing")
    public String ticketing(Model model) {
        return "ticketing";
    }

    @GetMapping("/advertise")
    public String advertise(Model model) {
        return "advertise";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        return "privacy";
    }

    @GetMapping("/rank")
    public String RankPage(Model model) {
        return "redirect:/ticketing";
    }

    @GetMapping("/reservation")
    public String reservationPage(@RequestParam(required = false) String token, Model model) {
        if (!ticketQueueService.isValidReservationToken(token)) {
            return "redirect:/ticketing";
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

        // 태그는 화면에 보이는 값이라 정규화한 것을 내려준다 — 주소창에 `?tag=IVE` 를 쳐도
        // 칩은 실제로 걸린 필터인 `ive` 가 켜져 보여야 한다
        String tag = TagNormalizer.normalizeOne(condition.getTag());
        // 정렬도 화면에 보이는 값이라 정규화해서 내려준다 — 알 수 없는 sort 로 들어와도
        // 선택 표시(current)와 canonical 판단은 항상 유효한 값(기본 latest)을 기준으로 해야 한다
        String sort = PostQueryCondition.PostSortType.from(condition.getSort()).name().toLowerCase();

        model.addAttribute("posts", posts);
        // 번호를 전부 뿌리면 글이 는 만큼 링크도 늘어난다. 현재 블록만 계산해서 내려준다
        model.addAttribute("pageBlock", PageBlock.of(posts.getNumber(), posts.getTotalPages()));
        model.addAttribute("keyword", condition.getKeyword());
        model.addAttribute("tag", tag);
        model.addAttribute("sort", sort);
        model.addAttribute("popularTags", popularTagService.getPopularTags());
        // 주소 조립을 템플릿에 맡기면 빈 파라미터(`keyword=`)를 뺄 수 없다 — CommunityListLinks 참고
        model.addAttribute("links", new CommunityListLinks(condition.getKeyword(), tag, sort));
        return "community/list";
    }

    /**
     * "내 글" — 목록을 서버가 못 그리는 유일한 커뮤니티 화면이다.
     * 누가 나인지는 토큰을 가진 브라우저만 알기 때문에 껍데기만 내려주고 JS 가 채운다.
     */
    @GetMapping("/community/mine")
    public String communityMine() {
        return "community/mine";
    }

    @GetMapping("/community/write")
    public String communityWrite(Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("postId", null);
        // 추천 태그 칩. 직접 치게만 두면 같은 뜻의 태그가 제각각으로 쌓인다
        model.addAttribute("popularTags", popularTagService.getPopularTags());
        return "community/write";
    }

    @GetMapping("/community/edit/{id}")
    public String communityEdit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("isEdit", true);
        model.addAttribute("postId", id);
        model.addAttribute("popularTags", popularTagService.getPopularTags());
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
        // 블라인드 처리는 PostResponse 안에서 이미 끝났다(작성자 본인에게만 원문).
        // 여기서 한 번 더 가리면 가리는 규칙이 두 곳으로 갈라져 한쪽만 고쳐지는 날이 온다.
        model.addAttribute("renderedContent", PostContentRenderer.render(post.getContent()));
        // 댓글도 서버가 그린다. 색인 조건이 "추천 1 이상 또는 댓글 1 이상"이라(5-1항)
        // 크롤러가 댓글을 봐야 이 페이지를 색인할 값어치가 생긴다.
        model.addAttribute("comments", postCommentService.list(id, null));
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

    @GetMapping("/practice/i-ticket-new")
    public String practiceITicketNew(Model model) {
        return "practice/i_ticket_new";
    }

    @GetMapping("/practice/i-ticket-new/intro")
    public String practiceITicketNewIntro(Model model) {
        return "practice/i_ticket_new_intro";
    }
}
