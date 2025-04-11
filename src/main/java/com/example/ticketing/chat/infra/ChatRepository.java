package com.example.ticketing.chat.infra;

import com.example.ticketing.chat.ChatTemp;
import org.springframework.data.repository.CrudRepository;

public interface ChatRepository extends CrudRepository<ChatTemp, Long> {}
