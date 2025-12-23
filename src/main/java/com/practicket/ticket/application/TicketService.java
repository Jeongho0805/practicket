package com.practicket.ticket.application;

import com.practicket.common.auth.ClientInfo;
import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.TicketException;
import com.practicket.ticket.component.TicketTimer;
import com.practicket.ticket.domain.Ticket;
import com.practicket.ticket.dto.response.TicketRankResponseDto;
import com.practicket.ticket.dto.request.TicketRequestDto;
import com.practicket.ticket.infra.redis.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketTimer ticketTimer;

    public void adjustStartTime() {
        ticketTimer.adjustStartTime();
    }

    public void initData() {
        ticketRepository.deleteAll();
    }

    public void validateStartTime() {
        if (!ticketTimer.isValidStartTime()) {
            throw new TicketException(ErrorCode.TICKETING_TIME_IS_NOT_ALLOWED);
        }
    }

    public void createTicket(ClientInfo clientInfo, TicketRequestDto dto) {
        List<String> seats = dto.getSeats();
        String reservationToken = dto.getReservationToken();
        ticketRepository.createTickets(clientInfo.getToken(), clientInfo.getName(), seats, reservationToken);
    }

    public List<TicketRankResponseDto> getRankInfo() {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt))
                .map(TicketRankResponseDto::createFromTicket)
                .toList();
    }

    public List<String> findAllSeats() {
        return ticketRepository.findAll()
                .stream()
                .map(Ticket::getSeat)
                .toList();
    }
}
