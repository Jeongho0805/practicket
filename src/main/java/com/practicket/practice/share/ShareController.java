package com.practicket.practice.share;

import com.practicket.practice.domain.PracticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.time.Duration;

/**
 * 공유 링크가 열리는 자리. 결과는 쿼리 파라미터에만 있고 저장하지 않는다.
 * 링크를 붙이면 카톡·X 가 og:image 를 긁어가 카드로 그려주고, 그 카드가 곧 유입 경로다.
 */
@Controller
@RequiredArgsConstructor
public class ShareController {

    private final ShareCardRenderer renderer;

    @GetMapping("/practice/result")
    public String result(@RequestParam(required = false) PracticeType type,
                         @RequestParam(defaultValue = "0") int total,
                         @RequestParam(defaultValue = "0") int reaction,
                         @RequestParam(defaultValue = "0") int queue,
                         @RequestParam(defaultValue = "0") int seat,
                         @RequestParam(defaultValue = "0") int rank,
                         @RequestParam(required = false) Integer pct,
                         Model model) {
        ShareResult r = ShareResult.of(type, total, reaction, queue, seat, rank, pct);
        model.addAttribute("r", r);
        model.addAttribute("query", query(r));
        model.addAttribute("customOg", true);
        return "practice/result";
    }

    @GetMapping(value = "/practice/result/og.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> ogImage(@RequestParam(required = false) PracticeType type,
                                          @RequestParam(defaultValue = "0") int total,
                                          @RequestParam(defaultValue = "0") int reaction,
                                          @RequestParam(defaultValue = "0") int queue,
                                          @RequestParam(defaultValue = "0") int seat,
                                          @RequestParam(defaultValue = "0") int rank,
                                          @RequestParam(required = false) Integer pct) throws IOException {
        byte[] png = renderer.render(ShareResult.of(type, total, reaction, queue, seat, rank, pct));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(png);
    }

    private String query(ShareResult r) {
        StringBuilder sb = new StringBuilder()
                .append("type=").append(r.type().name())
                .append("&total=").append(r.totalMs())
                .append("&reaction=").append(r.reactionMs())
                .append("&queue=").append(r.queueMs())
                .append("&seat=").append(r.seatMs())
                .append("&rank=").append(r.queueInitialRank());
        if (r.percentile() != null) sb.append("&pct=").append(r.percentile());
        return sb.toString();
    }
}
