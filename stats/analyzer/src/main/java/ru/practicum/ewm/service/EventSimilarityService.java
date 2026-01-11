package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class EventSimilarityService {

    private final EventSimilarityRepository repository;
    private final UserActionService userActionService;

    public void saveEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = new EventSimilarity();
        eventSimilarity.setEventA(eventSimilarityAvro.getEventA());
        eventSimilarity.setEventB(eventSimilarityAvro.getEventB());
        eventSimilarity.setScore(eventSimilarityAvro.getScore());
        eventSimilarity.setEventTime(eventSimilarityAvro.getTimestamp());
        repository.save(eventSimilarity);
    }

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        List<EventSimilarity> list = new ArrayList<>(getSimilarByEventAndUserId(request.getEventId(), request.getUserId()));
        list.sort(Comparator.comparing(EventSimilarity::getScore));
        return list.stream()
                .limit(request.getMaxResults())
                .map(item -> {
                    Long eventId = item.getEventA();
                    if (eventId == request.getEventId()) {
                        eventId = item.getEventB();
                    }
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(item.getScore())
                            .build();
                })
                .toList();
    }

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        List<UserAction> usersActionsList = userActionService.getUserActionByUserId(request.getUserId());
        if (usersActionsList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = usersActionsList.stream().map(UserAction::getEventId).toList();
        Map<Long, Double> eventsWeights = new HashMap<>();
        for (UserAction userAction : usersActionsList) {
            double weight = 0;
            switch (userAction.getActionType()) {
                case LIKE -> weight = 1;
                case REGISTER -> weight = 0.8;
                case VIEW -> weight = 0.4;
            }
            eventsWeights.put(userAction.getEventId(), weight);
        }

        usersActionsList.sort(Comparator.comparing(UserAction::getActionTime));
        List<UserAction> actionList = usersActionsList.stream().limit(request.getMaxResults()).toList();

        Set<RecommendedEventProto> recommendedEvents = new HashSet<>();
        for (UserAction userAction : actionList) {
            recommendedEvents.addAll(getSimilarByEventAndUserId(userAction.getEventId(), userAction.getUserId())
                    .stream()
                    .map(item -> {
                        long eventId = item.getEventB();
                        if (!item.getEventA().equals(userAction.getEventId())) {
                            eventId = item.getEventA();
                        }
                        return RecommendedEventProto.newBuilder()
                                .setEventId(eventId)
                                .setScore(0)
                                .build();
                    }).toList());
        }

        List<RecommendedEventProto> recomendationsList = recommendedEvents.stream()
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(request.getMaxResults()).toList();

        List<RecommendedEventProto> result = new ArrayList<>();
        for (RecommendedEventProto event : recommendedEvents) {
            List<RecommendedEventProto> nList = repository.findByEventAOrEventB(event.getEventId(), event.getEventId()).stream()
                    .filter(item -> ids.contains(item.getEventB()) || ids.contains(item.getEventA()))
                    .map(k -> {
                        long eventId = k.getEventB();
                        if (k.getEventA() != event.getEventId()) eventId = k.getEventA();
                        return RecommendedEventProto.newBuilder()
                                .setEventId(eventId)
                                .setScore(k.getScore())
                                .build();
                    }).toList();


            AtomicReference<Double> sum = new AtomicReference<>(0d);
            AtomicReference<Double> k = new AtomicReference<>(0d);
            nList.stream().limit(request.getMaxResults())
                    .forEach(x -> sum.set(sum.get() + (x.getScore() * eventsWeights.get(x.getEventId()))));
            nList.stream().limit(request.getMaxResults())
                    .forEach(x -> k.set(k.get() + x.getScore()));
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(event.getEventId())
                    .setScore(sum.get() / k.get())
                    .build());
        }
        return result;
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<RecommendedEventProto> result = new ArrayList<>();
        for (Long id : request.getEventIdList()) {
            AtomicReference<Double> sum = new AtomicReference<>(0d);
            List<UserAction> userActionList = userActionService.getUserActionByEventId(id);
            userActionList.stream()
                    .map(item -> {
                        double weiht = 0;
                        switch (item.getActionType()) {
                            case LIKE -> weiht = 1;
                            case REGISTER -> weiht = 0.8;
                            case VIEW -> weiht = 0.4;
                        }
                        return weiht;
                    }).forEach(item -> sum.set(sum.get() + item));
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(id)
                    .setScore(sum.get())
                    .build());
        }
        return result;
    }

    private List<EventSimilarity> getSimilarByEventAndUserId(Long eventId, Long userId) {
        List<Long> actionsIds = userActionService.getUserActionByUserId(userId).stream()
                .map(UserAction::getEventId)
                .toList();
        return repository.findByEventAOrEventB(eventId, eventId).stream()
                .filter(item -> !actionsIds.contains(item.getEventA())
                        || !actionsIds.contains(item.getEventB()))
                .toList();
    }
}
