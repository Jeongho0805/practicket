package com.practicket.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientResponse {

    private String name;

    private Boolean banned;
}
