package com.example.ticketing.chat.infra;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMongoRepository extends MongoRepository<ChatDocument, ObjectId> {}
