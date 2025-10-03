package com.example.ticketing.art.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "설명을 입력해주세요.")
    @Size(max = 1000, message = "설명은 1000자 이하로 입력해주세요.")
    private String description;

    @NotBlank(message = "픽셀 데이터를 입력해주세요.")
    private String pixelData;

    @NotNull(message = "가로 크기를 입력해주세요.")
    @Positive(message = "가로 크기는 양수여야 합니다.")
    private Integer width;

    @NotNull(message = "세로 크기를 입력해주세요.")
    @Positive(message = "세로 크기는 양수여야 합니다.")
    private Integer height;

    private Boolean isPublic = true;
}