package com.example.ticketing.chat.infra;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMongoRepository extends MongoRepository<ChatDocument, ObjectId> {

    List<ChatDocument> findTop50ByCreatedAtBeforeOrderByCreatedAtDesc(LocalDateTime before);
}
