package com.practicket.ticket.component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Getter
@Component
public class VirtualNameLoader {

    private List<String> names;

    @PostConstruct
    public void load() throws IOException {
        ClassPathResource resource = new ClassPathResource("etc/names.txt");
        names = Files.readAllLines(resource.getFile().toPath());
    }
}
