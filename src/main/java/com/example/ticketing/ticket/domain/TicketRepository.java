package com.example.ticketing.ticket.domain;


import org.springframework.data.repository.CrudRepository;

public interface TicketRepository extends CrudRepository<Ticket, String> {}
