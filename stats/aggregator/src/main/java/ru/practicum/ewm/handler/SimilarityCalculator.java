package ru.practicum.ewm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.config.AggregatorKafkaConfig;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalculator {
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final AggregatorKafkaConfig aggregatorKafkaConfig;
    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;

    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventMinWeightsSum = new HashMap<>();

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                      KafkaConsumer<String, UserActionAvro> consumer) {
        currentOffsets.put(new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, e) -> {
                if (e != null) {
                    log.warn("Offstes fixation exception: {}", offsets, e);
                }
            });
        }
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            consumer.subscribe(List.of(aggregatorKafkaConfig.getConsumerTopic()));
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(aggregatorKafkaConfig.getConsumeAttemptTimeout());
                int count = 0;
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    handleAction(record.value());
                    manageOffsets(record, count, consumer);
                    count++;
                }
                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {

        } catch (
                Exception e) {
            log.error("Events processing exception", e);
        } finally {

            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }

    private double getWeightByType(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    private void handleAction(UserActionAvro userActionAvro) {
        long userId = userActionAvro.getUserId();
        long eventId = userActionAvro.getEventId();
        double weight = getWeightByType(userActionAvro.getActionType());
        double oldWeight = userEventWeights.computeIfAbsent(eventId, e -> new HashMap<>())
                .getOrDefault(userId, 0.0);
        if (oldWeight >= weight) {
            return;
        }
        userEventWeights.computeIfAbsent(eventId, e -> new HashMap<>())
                .merge(userId, weight, Math::max);

        double newTotalWeight = updateTotalWeigh(eventId, oldWeight, weight);
        for (Long eventBId : userEventWeights.keySet()) {
            if (eventBId == eventId || !userEventWeights.get(eventBId).containsKey(userId)) {
                continue;
            }
            double eventBWeight = userEventWeights.get(eventBId).get(userId);
            double oldMin = Math.min(oldWeight, eventBWeight);
            double newMin = Math.min(weight, eventBWeight);
            double delta = newMin - oldMin;
            double min = getMin(eventBId, eventId);
            double minWeightSum = min;
            if (delta != 0) {
                double newMinSum = min + delta;
                minWeightSum = newMinSum;
                setMin(eventBId, eventId, newMinSum);
            }
            double otherEventWeight = eventWeightSums.getOrDefault(eventBId, 0.0);
            double similarity = calculateSimilarity(minWeightSum, newTotalWeight, otherEventWeight);
            pullSimilarity(eventId, eventBId, similarity, userActionAvro.getTimestamp());
        }
    }

    private double updateTotalWeigh(long eventId, double oldWeight, double newWeight) {
        double oldTotal = eventWeightSums.getOrDefault(eventId, 0.0);
        double delta = newWeight - oldWeight;
        double newTotal = oldTotal + delta;
        eventWeightSums.put(eventId, newTotal);
        return newTotal;
    }

    public void setMin(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        eventMinWeightsSum.computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    public double getMin(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return eventMinWeightsSum.computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private double calculateSimilarity(double minWeightSum, double totalEventWeight,
                                       double totalOtherEventWeight) {
        return minWeightSum / (Math.sqrt(totalEventWeight) * Math.sqrt(totalOtherEventWeight));
    }

    private void pullSimilarity(long eventA, long eventB, double similarity, Instant timestamp) {
        EventSimilarityAvro eventSimilarity = EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(eventA, eventB))
                .setEventB(Math.max(eventA, eventB))
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();
        producer.send(new ProducerRecord<>(aggregatorKafkaConfig.getProducerTopic(), eventSimilarity));
    }

    public void stop() {
        consumer.wakeup();
    }
}
