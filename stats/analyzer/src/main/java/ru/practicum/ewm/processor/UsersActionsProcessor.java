package ru.practicum.ewm.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.config.KafkaConsumerActionsConfig;
import ru.practicum.ewm.config.KafkaTopicConfig;
import ru.practicum.ewm.service.UserActionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsersActionsProcessor implements Runnable {

    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final KafkaConsumerActionsConfig consumerConfig;
    private final KafkaTopicConfig topicsConfig;
    private final UserActionService service;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(topicsConfig.getActions());
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(consumerConfig.getConsumeAttemptTimeoutMs()));
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    service.saveActionType(record.value());
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка при обработки событий user-actions", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}