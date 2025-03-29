package com.example.ticketing.ticket;

import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.exception.TicketException;
import com.example.ticketing.common.auth.UserInfo;
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
            throw new TicketException(ErrorCode.TICKETING_TIME_IS_NOT_ALLOWED);
        }
    }

    @Transactional
    public void issueTicket(UserInfo userInfo, TicketRequestDto dto) {
        List<String> seats = dto.getSeats();
        ticketCounter.isAvailableCount(seats.size());
        ticketManager.createTickets(userInfo.getKey(), userInfo.getName(), seats);
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
