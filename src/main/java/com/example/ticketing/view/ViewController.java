package com.example.ticketing.view;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
public class ViewController {

    @Value("${app.host}")
    private String host;

    @GetMapping("/")
    public String main(Model model) {
        model.addAttribute("host", host);
        return "main";
    }

    @GetMapping("/rank")
    public String RankPage(Model model) {
        model.addAttribute("host", host);
        return "rank";
    }

    @GetMapping("/reservation")
    public String reservationPage(Model model) {
        model.addAttribute("host", host);
        return "reservation";
    }

    @GetMapping("/security")
    public String securityLetterPage(Model model) {
        model.addAttribute("host", host);
        return "security";
    }

    @GetMapping("/blog")
    public String blogList(Model model) {
        model.addAttribute("host", host);
        return "blog";
    }

    @GetMapping("/blog/{id}")
    public String blogContents(@PathVariable("id") String id, Model model) {
        model.addAttribute("host", host);
        return "blog/" + id;
    }
}
