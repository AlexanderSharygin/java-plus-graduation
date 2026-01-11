package ru.practicum.ewm.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.config.KafkaConsumerActionsConfig;
import ru.practicum.ewm.config.KafkaConsumerSimilarityConfig;
import ru.practicum.ewm.config.KafkaTopicConfig;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.service.EventSimilarityService;
import ru.practicum.ewm.service.UserActionService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor implements Runnable {

    private final KafkaConsumer<String, EventSimilarityAvro> consumer;
    private final KafkaConsumerSimilarityConfig consumerConfig;
    private final KafkaTopicConfig topicsConfig;
    private final EventSimilarityService service;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(topicsConfig.getActions());
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(consumerConfig.getConsumeAttemptTimeoutMs()));
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    service.saveEventSimilarity(record.value());
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка при обработки событий event-similarity", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}