package com.practicket.ticket.dto.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequestDto {
    @NotEmpty(message = "좌석 선택은 필수입니다.")
    private List<String> seats;

    @NotEmpty(message = "예매 권한 토큰이 필요합니다.")
    private String reservationToken;
}
