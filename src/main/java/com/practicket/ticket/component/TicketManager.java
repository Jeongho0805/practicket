package com.practicket.ticket.component;

import com.practicket.common.exception.ErrorCode;
import com.practicket.common.exception.TicketException;
import com.practicket.ticket.domain.Ticket;
import com.practicket.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class TicketManager {

    private final TicketRepository ticketRepository;

    public void deleteAll() {
        ticketRepository.deleteAll();
    }

    public List<Ticket> findAll() {
        return StreamSupport
                .stream(ticketRepository.findAll().spliterator(), false)
                .toList();
    }


    public void createTickets(String key, String name, List<String> seats) {
        validateExistSeat(seats);
        List<Ticket> tickets = seats.stream()
                .map((seat) -> new Ticket(key, name, seat))
                .toList();
        ticketRepository.saveAll(tickets);
    }

    public void createTicketForAI(String name) {
        int rowSize = 10;
        int colSize = 10;
        Random random = new Random();
        int row = random.nextInt(rowSize);
        int col = random.nextInt(colSize);
        String seat =  (char) ('A' + row) + String.valueOf(col + 1);
        ticketRepository.save(new Ticket(name, name, seat));
    }

    private void validateExistSeat(List<String> seats) {
        Iterable<Ticket> tickets = ticketRepository.findAll();
        for (Ticket ticket : tickets) {
            if (seats.contains(ticket.getSeat())) {
                throw new TicketException(ErrorCode.SEAT_ALREADY_BOOKED);
            }
        }
    }
}
