package com.practicket.notice.application;

import com.practicket.notice.domain.Notice;
import com.practicket.notice.domain.NoticeType;
import com.practicket.notice.dto.NoticeListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 응답 형태({@code data} + {@code has_next})는 랭킹 API 와 맞춘다 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeApiController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) NoticeType type,
            @RequestParam(defaultValue = "0") int page) {

        Page<Notice> slice = noticeService.getPublishedPage(type, page);
        List<NoticeListResponse> data = slice.getContent().stream()
                .map(NoticeListResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "data", data,
                "has_next", slice.hasNext()
        ));
    }
}
