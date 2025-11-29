package com.practicket.art.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtUpdateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 20, message = "제목은 20자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "픽셀 데이터를 입력해주세요.")
    private String pixelData;
}