package com.example.ticketing.ticket;

import com.example.ticketing.ticket.component.TicketCounter;
import com.example.ticketing.ticket.component.TicketManager;
import com.example.ticketing.ticket.component.TicketTimer;
import com.example.ticketing.ticket.domain.Ticket;
import com.example.ticketing.ticket.domain.TicketRepository;
import com.example.ticketing.ticket.dto.TicketRankDto;
import com.example.ticketing.ticket.dto.TicketRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.StreamSupport;


@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketCounter ticketCounter;

    private final TicketTimer ticketTimer;

    private final TicketManager ticketManager;

    private final TicketRepository ticketRepository;

    public void resetTimer() {
        ticketTimer.resetStartTime();
    }

    public void initData() {
        ticketRepository.deleteAll();
        ticketCounter.resetCount();
    }

    public void validateStartTime() {
        if (!ticketTimer.isValidStartTime()) {
            throw new RuntimeException("티켓팅 시작 시간이 아닙니다.");
        }
    }

    @Transactional
    public void issueTicket(TicketRequestDto dto) {
        List<String> seats = dto.getSeats();
        ticketCounter.isAvailableCount(seats.size());
        ticketManager.createTickets(seats, dto.getName());
        ticketCounter.minusTicketCount(seats.size());
    }

    public List<TicketRankDto> getRankInfo() {
        Iterable<Ticket> tickets = ticketRepository.findAll();
        return StreamSupport
                .stream(tickets.spliterator(), false)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Ticket::getCreatedAt))
                .map(TicketRankDto::createFromTicket).toList();
    }

    public List<String> findAllSeats() {
        List<String> seats = new ArrayList<>();
        ticketRepository.findAll().forEach((ticket -> seats.add(ticket.getSeat())));
        return seats;
    }
}
