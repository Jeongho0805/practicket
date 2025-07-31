package com.example.ticketing.view;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
public class ViewController {

    @GetMapping("/")
    public String main(Model model) {
        return "main";
    }

    @GetMapping("/rank")
    public String RankPage(Model model) {
        return "rank";
    }

    @GetMapping("/reservation")
    public String reservationPage(Model model) {
        return "reservation";
    }

    @GetMapping("/security")
    public String securityLetterPage(Model model) {
        return "security";
    }

    @GetMapping("/blog")
    public String blogList(Model model) {
        return "blog";
    }

    @GetMapping("/blog/{id}")
    public String blogContents(@PathVariable("id") String id, Model model) {
        return "blog/" + id;
    }
}
