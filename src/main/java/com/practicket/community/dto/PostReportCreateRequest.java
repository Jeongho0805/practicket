package com.practicket.community.dto;

import com.practicket.community.domain.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 자유 입력 칸은 만들지 않는다 — 그 칸이 또 하나의 욕설 입력창이 된다 */
@Getter
@Setter
@NoArgsConstructor
public class PostReportCreateRequest {

    @NotNull(message = "신고 사유를 선택해주세요.")
    private ReportReason reason;
}
