package com.example.ticketing.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ViewControllerAdvice {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${app.host}")
    private String host;

    @ModelAttribute("activeProfile")
    public String getActiveProfile() {
        return activeProfile;
    }

    @ModelAttribute("host")
    public String getHost() {
        return host;
    }
}
