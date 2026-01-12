package ru.practicum.ewm.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ru.practicum.ewm.config.KafkaTopicConfig;
import ru.practicum.ewm.producer.KafkaProducer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Service
@RequiredArgsConstructor
public class EventSimilarityProcessor {

    protected final KafkaProducer producer;
    private final KafkaTopicConfig kafkaConfig;

    public void collectEventSimilarity(EventSimilarityAvro request) {
        String topic = kafkaConfig.producerTopic();
        producer.send(request, topic);
    }
}
