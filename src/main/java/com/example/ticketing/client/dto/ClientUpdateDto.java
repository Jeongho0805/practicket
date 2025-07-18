package com.example.ticketing.client.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ClientUpdateDto {

    @Pattern(regexp = "^$|^.{1,10}$", message = "닉네임은 1~10자 또는 null이어야 합니다.")
    private String name;
}
