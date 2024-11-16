package com.example.ticketing.view;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class ViewController {

    @GetMapping("/")
    public String main() {
        return "forward:/page/main.html";
    }

    @GetMapping("/rank")
    public String RankPage() {
        return "forward:/page/rank.html";
    }

    @GetMapping("/reservation")
    public String reservationPage() {
        return "forward:/page/reservation.html";
    }
}
