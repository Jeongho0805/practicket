package com.practicket.ticket.component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Getter
@Component
public class VirtualNameLoader {

    @Value("classpath:etc/names.txt")
    private Resource nameResource;

    private List<String> names;

    @PostConstruct
    public void load() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(nameResource.getInputStream()))) {
            names = reader.lines().toList();
        }
    }
}
