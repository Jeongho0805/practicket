package com.example.ticketing.ticket.application;

import com.example.ticketing.common.auth.ClientInfo;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.exception.TicketException;
import com.example.ticketing.ticket.component.TicketCounter;
import com.example.ticketing.ticket.component.TicketManager;
import com.example.ticketing.ticket.component.TicketTimer;
import com.example.ticketing.ticket.domain.Ticket;
import com.example.ticketing.ticket.dto.TicketRankDto;
import com.example.ticketing.ticket.dto.TicketRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TicketService {

    private final TicketCounter ticketCounter;

    private final TicketTimer ticketTimer;

    private final TicketManager ticketManager;

    public void resetTimer() {
        ticketTimer.resetStartTime();
    }

    public void initData() {
        ticketManager.deleteAll();
        ticketCounter.resetCount();
    }

    public void validateStartTime() {
        if (!ticketTimer.isValidStartTime()) {
            throw new TicketException(ErrorCode.TICKETING_TIME_IS_NOT_ALLOWED);
        }
    }

    public void issueTicket(ClientInfo userInfo, TicketRequestDto dto) {
        List<String> seats = dto.getSeats();
        ticketCounter.isAvailableCount(seats.size());
        ticketManager.createTickets(userInfo.getToken(), userInfo.getName(), seats);
        ticketCounter.minusTicketCount(seats.size());
    }

    public List<TicketRankDto> getRankInfo() {
        List<Ticket> tickets = ticketManager.findAll();
        return tickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt))
                .map(TicketRankDto::createFromTicket)
                .toList();
    }

    public List<String> findAllSeats() {
        return ticketManager.findAll()
                .stream()
                .map(Ticket::getSeat)
                .toList();
    }
}
