package com.practicket.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostCommentCreateRequest {

    /** 컬럼은 TEXT 라 잘릴 걱정은 없다. 한 댓글로 화면을 도배하는 걸 막는 용도 */
    @NotBlank(message = "댓글을 입력해주세요.")
    @Size(max = 1000, message = "댓글은 1000자까지 쓸 수 있습니다.")
    private String content;
}
