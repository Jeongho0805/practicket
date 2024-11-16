package com.example.ticketing.ticket;

import com.example.ticketing.ticket.dto.TicketRankDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private final TicketCreator ticketCreator;

    private final TicketTimer ticketTimer;

    private final TicketRepository ticketRepository;

    private final TicketQueueEventRepository ticketQueueRepository;

    public void resetTimer() {
        ticketTimer.resetStartTime();
        LocalDateTime startTime = ticketTimer.getStartTime();
        log.info("[ Server Log ] : 변경된 티켓 예매 시작 시간 = {}", startTime);
    }

    public void initData() {
        ticketRepository.deleteAll();
        ticketCreator.resetCount();
    }

    public void validateRegisterAvailable() {
        validateStartTime();
        validateTicketCount();
    }

    public void validateTicketCount() {
        int ticketCount = ticketCreator.getTicketCount();
        int waitingCount = ticketQueueRepository.getListSize();
        if (ticketCount <= waitingCount) {
            throw new RuntimeException("티켓이 모두 소진되었습니다.");
        }

    }

    public void validateStartTime() {
        if (!ticketTimer.isValidStartTime()) {
            throw new RuntimeException("티켓팅 시작 시간이 아닙니다.");
        }
    }

    public void issueTicket(String name) {
        String ticketCode = ticketCreator.RegisterTicketCode();
        Ticket ticket = new Ticket(name, ticketCode);
        ticketRepository.save(ticket);
    }

    public List<TicketRankDto> getRankInfo() {
        Iterable<Ticket> tickets = ticketRepository.findAll();
        return StreamSupport
                .stream(tickets.spliterator(), false)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Ticket::getCreatedAt))
                .map(TicketRankDto::createFromTicket).toList();
    }

    public boolean isAlreadyIssued(String name) {
        Optional<Ticket> ticket = ticketRepository.findById(name);
        return ticket.isPresent();
    }
}
