package com.practicket.inquiry.dto;

import com.practicket.inquiry.domain.InquiryType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InquiryRequest {

    @NotNull(message = "문의 유형을 선택해주세요.")
    private InquiryType type;

    @NotBlank(message = "회신 받을 이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 255, message = "이메일이 너무 깁니다.")
    private String email;

    @NotBlank(message = "문의 내용을 입력해주세요.")
    @Size(min = 5, max = 5000, message = "문의 내용은 5자 이상 5000자 이하로 입력해주세요.")
    private String content;
}
