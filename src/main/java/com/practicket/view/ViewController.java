package com.practicket.view;

import com.practicket.ticket.application.TicketQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final TicketQueueService ticketQueueService;

    @GetMapping("/")
    public String main(Model model) {
        return "main";
    }

    @GetMapping("/rank")
    public String RankPage(Model model) {
        return "rank";
    }

    @GetMapping("/reservation")
    public String reservationPage(@RequestParam(required = false) String token, Model model) {
        if (!ticketQueueService.isValidReservationToken(token)) {
            return "redirect:/";
        }
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

    @GetMapping("/art")
    public String artGallery(Model model) {
        return "art/gallery";
    }

    @GetMapping("/art/create")
    public String artCreate(Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("artId", null);
        return "art/create";
    }

    @GetMapping("/art/edit/{id}")
    public String artEdit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("isEdit", true);
        model.addAttribute("artId", id);
        return "art/create";
    }

    @GetMapping("/art/{id}")
    public String artDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("artId", id);
        return "art/detail";
    }
}
