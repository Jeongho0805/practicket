package com.example.ticketing.chat.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequestDto {

    @NotNull(message = "채팅을 입력해주세요.")
    @NotBlank(message = "공백은 입력 불가합니다.")
    @Size(min = 1, max = 100, message = "채팅은 최대 100 글자까지 입력 가능합니다.")
    private String text;
}
