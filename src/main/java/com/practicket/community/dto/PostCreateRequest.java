package com.practicket.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 5000, message = "내용은 5000자 이하로 입력해주세요.")
    private String content;

    /** 기기를 바꿔 토큰이 사라졌을 때 쓰는 비상 열쇠. 네 자리 숫자만 받는다(Q5). */
    @NotBlank(message = "삭제 비밀번호를 입력해주세요.")
    @Pattern(regexp = "^\\d{4}$", message = "삭제 비밀번호는 숫자 네 자리로 입력해주세요.")
    private String deletePassword;
}
