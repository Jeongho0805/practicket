package com.example.ticketing.common.auth;

import lombok.*;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientInfo {

    private Long clientId;

    private String token;

    private String name;

    private Boolean banned;
}
