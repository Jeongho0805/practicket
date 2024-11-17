package com.example.ticketing.ticket.component;

import com.example.ticketing.ticket.domain.Ticket;
import com.example.ticketing.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketManager {

    private final TicketRepository ticketRepository;


    public void createTickets(List<String> seats, String name) {
        validateExistSeat(seats);
        List<Ticket> tickets = seats.stream()
                .map((seat) -> new Ticket(seat, name))
                .toList();
        ticketRepository.saveAll(tickets);
    }

    private void validateExistSeat(List<String> seats) {
        Iterable<Ticket> tickets = ticketRepository.findAll();
        for (Ticket ticket : tickets) {
            if (seats.contains(ticket.getSeat())) {
                throw new IllegalArgumentException("이미 선택된 좌석입니다.");
            }
        }
    }
}
