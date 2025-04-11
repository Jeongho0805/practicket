package com.example.ticketing.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {

    @NotNull(message = "닉네임은 필수 입력 값 입니다.")
    @NotBlank(message = "공백 입력은 불가합니다.")
    @Size(min = 1, max = 10, message = "닉네임은 최대 10 글자까지 입력가능합니다.")
    private String name;
}
