package com.example.ticketing.ticket.domain;


import com.example.ticketing.ticket.domain.Ticket;
import org.springframework.data.repository.CrudRepository;

public interface TicketRepository extends CrudRepository<Ticket, String> {}
