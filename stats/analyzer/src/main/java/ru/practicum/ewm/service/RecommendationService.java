package ru.practicum.ewm.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.practicum.ewm.models.EventSimilarity;
import ru.practicum.ewm.models.UserAction;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.repository.UserActionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAOrEventB(request.getEventId(), request.getEventId());

        Set<Long> userInteractions = userActionRepository.findByUserId(request.getUserId())
                .stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> toSort = similarities.stream().filter(k -> !userInteractions.contains(k.getEventA())
                || !userInteractions.contains(k.getEventB())).sorted(Comparator.comparing(EventSimilarity::getScore).reversed()).collect(Collectors.toList());
        long limit = request.getMaxResults();
        for (EventSimilarity k : toSort) {
            if (limit-- == 0) break;
            long recommendedEvent = k.getEventA().equals(request.getEventId()) ? k.getEventB() : k.getEventA();
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(recommendedEvent)
                    .setScore(k.getScore())
                    .build());
        }
        responseObserver.onCompleted();
    }

    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        List<UserAction> interactions = userActionRepository.findByUserId(request.getUserId());
        if (interactions.isEmpty()) {
            responseObserver.onCompleted();
            return;
        }
        Set<Long> recentEvents = interactions.stream()
                .sorted(Comparator.comparing(UserAction::getTimestamp).reversed())
                .limit(request.getMaxResults())
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());
        List<EventSimilarity> similarities = recentEvents.stream().flatMap(eventId -> eventSimilarityRepository.findByEventAOrEventB(eventId, eventId).stream()).collect(Collectors.toList());
        Set<Long> userInteractions = interactions.stream().map(UserAction::getEventId).collect(Collectors.toSet());
        List<EventSimilarity> toSort = similarities.stream().filter(k -> !userInteractions.contains(k.getEventA())
                || !userInteractions.contains(k.getEventB())).sorted(Comparator.comparing(EventSimilarity::getScore).reversed()).collect(Collectors.toList());
        long limit = request.getMaxResults();
        for (EventSimilarity k : toSort) {
            if (limit-- == 0) break;
            long recommendedEvent = userInteractions.contains(k.getEventA()) ? k.getEventB() : k.getEventA();
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(recommendedEvent)
                    .setScore(k.getScore())
                    .build());
        }

        responseObserver.onCompleted();
    }

    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        for (long eventId : request.getEventIdList()) {
            double totalWeight = userActionRepository.findByEventId(eventId)
                    .stream()
                    .mapToDouble((uah) -> switch (uah.getActionType()) {
                        case VIEW -> 0.4;
                        case REGISTER -> 0.8;
                        case LIKE -> 1.0;
                    })
                    .sum();

            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(totalWeight)
                    .build());
        }
        responseObserver.onCompleted();
    }
}
