package com.example.ticketing.common.auth;

import com.example.ticketing.auth.dto.SessionObject;
import lombok.*;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo {

    private String ip;

    private String name;

    private String key;
}
