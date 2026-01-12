package ru.practicum.ewm;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;


import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        List<RecommendedEventProto> similarEvents = new ArrayList<>();
        client.getSimilarEvents(request)
                .forEachRemaining(similarEvents::add);

        return similarEvents;
    }

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, Long maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        List<RecommendedEventProto> recommendations = new ArrayList<>();
        client.getRecommendationsForUser(request)
                .forEachRemaining(recommendations::add);

        return recommendations;
    }

    public Map<Long, Double> getInteractionsCount(List<Long> ids) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(ids)
                .build();

        Map<Long, Double> interactionsCount = new HashMap<>();
        client.getInteractionsCount(request).forEachRemaining(event -> interactionsCount.put(event.getEventId(), event.getScore()));

        return interactionsCount;
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
