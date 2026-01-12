package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.processor.EventSimilarityProcessor;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final EventSimilarityProcessor eventSimilarityProcessor;

    private final Map<Long, Map<Long, Double>> eventWeight = new HashMap<>();
    private final Map<Long, Double> eventWeightSum = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSum = new HashMap<>();


    public void calculateWeight(UserActionAvro userActionAvro) {
        double weiht = 0;
        switch (userActionAvro.getActionType()) {
            case LIKE -> weiht = 1;
            case REGISTER -> weiht = 0.8;
            case VIEW -> weiht = 0.4;
        }
        if (eventWeight.containsKey(userActionAvro.getEventId())) {
            if (eventWeight.get(userActionAvro.getEventId()).containsKey(userActionAvro.getUserId())) {
                eventWeight.get(userActionAvro.getEventId())
                        .put(userActionAvro.getUserId(),
                                Math.min(weiht, eventWeight.get(userActionAvro.getEventId()).get(userActionAvro.getUserId())));
            } else return;
        } else {
            eventWeight.put(userActionAvro.getEventId(), new HashMap<>());
            eventWeight.get(userActionAvro.getEventId()).put(userActionAvro.getUserId(), weiht);
        }

        AtomicReference<Double> sum = new AtomicReference<>(0d);
        eventWeight.get(userActionAvro.getEventId()).values().forEach(x -> sum.set(sum.get() + x));

        eventWeightSum.put(userActionAvro.getEventId(), sum.get());


        for (Long l : eventWeight.keySet()) {
            if (l == userActionAvro.getEventId()) continue;
            if (eventWeight.get(l).containsKey(userActionAvro.getUserId())) {
                double minW = Math.min(eventWeight.get(l).get(userActionAvro.getUserId()),
                        eventWeight.get(userActionAvro.getEventId()).get(userActionAvro.getUserId()));
                put(userActionAvro.getEventId(), l, minW);

                send(userActionAvro.getEventId(), l, minW, userActionAvro.getTimestamp());
            }
        }
    }

    public void put(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSum
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    public double get(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSum
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private void send(long eventA, long eventB, double sum, Instant instant) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        EventSimilarityAvro eventSimilarityAvro = new EventSimilarityAvro(first, second, sum, instant);
        eventSimilarityProcessor.collectEventSimilarity(eventSimilarityAvro);

    }
}
