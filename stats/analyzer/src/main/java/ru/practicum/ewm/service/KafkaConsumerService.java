package ru.practicum.ewm.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface KafkaConsumerService {
    void userActionReceived(UserActionAvro userActionAvro);

    void eventsSimilarityReceived(EventSimilarityAvro eventSimilarityAvro);
}
