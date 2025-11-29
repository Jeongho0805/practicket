package com.practicket.chat.domain;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMongoRepository extends MongoRepository<Chat, ObjectId> {

    List<Chat> findTop50ByCreatedAtBeforeOrderByCreatedAtDesc(LocalDateTime before);
}
