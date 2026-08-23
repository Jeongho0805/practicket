package com.practicket.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostUpdateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 5000, message = "내용은 5000자 이하로 입력해주세요.")
    private String content;

    /** 개수·글자수·허용문자는 TagNormalizer 가 맞추므로 여기서 막지 않는다 */
    private List<String> tags;
}
