package com.practicket.community.application;

import com.practicket.client.component.ClientInfoExtractor;
import com.practicket.common.auth.Auth;
import com.practicket.common.auth.ClientInfo;
import com.practicket.community.domain.entity.ReportTargetType;
import com.practicket.community.dto.PostReportCreateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 글·댓글을 한 컨트롤러에서 받는다 — PostReport 가 둘을 한 테이블에 담기 때문이다 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportService postReportService;
    private final ClientInfoExtractor clientInfoExtractor;

    @PostMapping("/posts/{postId}/reports")
    public ResponseEntity<Void> reportPost(
            @PathVariable Long postId,
            @Valid @RequestBody PostReportCreateRequest request,
            @Auth ClientInfo clientInfo,
            HttpServletRequest httpRequest) {
        String clientIp = clientInfoExtractor.extractClientInfo(httpRequest).getIp();
        postReportService.report(ReportTargetType.POST, postId, request, clientInfo, clientIp);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<Void> reportComment(
            @PathVariable Long commentId,
            @Valid @RequestBody PostReportCreateRequest request,
            @Auth ClientInfo clientInfo,
            HttpServletRequest httpRequest) {
        String clientIp = clientInfoExtractor.extractClientInfo(httpRequest).getIp();
        postReportService.report(ReportTargetType.COMMENT, commentId, request, clientInfo, clientIp);
        return ResponseEntity.noContent().build();
    }
}
