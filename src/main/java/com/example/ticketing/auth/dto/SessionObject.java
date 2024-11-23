package com.example.ticketing.auth.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionObject implements Serializable {

    private String ip;

    private String name;

    public SessionObject(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }
}
