package com.example.ticketing.view;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class ViewController {

    @GetMapping("/")
    public String main() {
        return "main";
    }

    @GetMapping("/rank")
    public String RankPage() {
        return "rank";
    }

    @GetMapping("/reservation")
    public String reservationPage() {
        return "reservation";
    }
}
