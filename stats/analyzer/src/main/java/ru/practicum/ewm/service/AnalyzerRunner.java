package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.config.AnalyserKafkaConfig;
import ru.practicum.ewm.handler.ConsumerHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerRunner {
    private static final Map<TopicPartition, OffsetAndMetadata> CURRENT_SIMILARITY_OFFSETS = new HashMap<>();
    private static final Map<TopicPartition, OffsetAndMetadata> CURRENT_ACTION_OFFSETS = new HashMap<>();

    private final ConsumerHandler consumerHandler;
    private final AnalyserKafkaConfig kafkaConfig;
    private final KafkaConsumer<String, EventSimilarityAvro> similarityConsumer;
    private final KafkaConsumer<String, UserActionAvro> actionsConsumer;

    private static void manageSimilarityOffsets(ConsumerRecord<String, EventSimilarityAvro> record, int count,
                                                KafkaConsumer<String, EventSimilarityAvro> consumer) {
        CURRENT_SIMILARITY_OFFSETS.put(new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
        if (count % 10 == 0) {
            consumer.commitAsync(CURRENT_SIMILARITY_OFFSETS, (offsets, e) -> {
                if (e != null) {
                    log.warn("Offstes fixation exception: {}", offsets, e);
                }
            });
        }
    }

    private static void manageActionOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                            KafkaConsumer<String, UserActionAvro> consumer) {
        CURRENT_ACTION_OFFSETS.put(new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
        if (count % 10 == 0) {
            consumer.commitAsync(CURRENT_ACTION_OFFSETS, (offsets, e) -> {
                if (e != null) {
                    log.warn("Offstes fixation exception: {}", offsets, e);
                }
            });
        }
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(similarityConsumer::wakeup));
        Runtime.getRuntime().addShutdownHook(new Thread(actionsConsumer::wakeup));
        try {
            similarityConsumer.subscribe(List.of(kafkaConfig.getSimilarityTopic()));
            actionsConsumer.subscribe(List.of(kafkaConfig.getUserActionTopic()));
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = similarityConsumer.poll(kafkaConfig.getConsumeAttemptTimeout());
                int similarityCount = 0;
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    consumerHandler.handleEventsSimilarity(record.value());
                    manageSimilarityOffsets(record, similarityCount, similarityConsumer);
                    similarityCount++;
                }
                similarityConsumer.commitAsync();

                ConsumerRecords<String, UserActionAvro> actionRecords = actionsConsumer.poll(kafkaConfig.getConsumeAttemptTimeout());
                int actionsCount = 0;
                for (ConsumerRecord<String, UserActionAvro> record : actionRecords) {
                    consumerHandler.handleUserAction(record.value());
                    manageActionOffsets(record, actionsCount, actionsConsumer);
                    actionsCount++;
                }
                similarityConsumer.commitAsync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Consumer processing exception", e);
        } finally {

            try {
                similarityConsumer.commitSync(CURRENT_SIMILARITY_OFFSETS);
                actionsConsumer.commitSync(CURRENT_ACTION_OFFSETS);
            } finally {
                similarityConsumer.close();
                actionsConsumer.close();
            }
        }
    }

    public void stop() {
        similarityConsumer.wakeup();
        actionsConsumer.wakeup();
    }
}