package com.practicket.ticket.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.TicketException;
import com.practicket.ticket.component.TicketCounter;
import com.practicket.ticket.component.TicketManager;
import com.practicket.ticket.component.TicketTimer;
import com.practicket.ticket.domain.Ticket;
import com.practicket.ticket.dto.TicketRankDto;
import com.practicket.ticket.dto.TicketRequestDto;
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
        ticketCounter.minusTicketCount(seats.size());
        try {
            ticketManager.createTickets(userInfo.getToken(), userInfo.getName(), seats);
        } catch (Exception e) {
            ticketCounter.plusTicketCount(seats.size());
            throw e;
        }
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
