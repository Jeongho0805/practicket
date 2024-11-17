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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;


@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketCounter ticketCounter;

    private final TicketTimer ticketTimer;

    private final TicketManager ticketManager;

    private final TicketRepository ticketRepository;

    private final TicketQueueEventRepository ticketQueueRepository;

    public void resetTimer() {
        ticketTimer.resetStartTime();
        LocalDateTime startTime = ticketTimer.getStartTime();
        log.info("[ Server Log ] : 변경된 티켓 예매 시작 시간 = {}", startTime);
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
}
