package com.practicket.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ClientRequestInfo {

    private String ip;

    private String device;

    private String sourceUrl;
}
