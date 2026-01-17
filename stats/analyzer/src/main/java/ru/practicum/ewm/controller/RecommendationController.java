package ru.practicum.ewm.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.grpc.stats.analyzer.RecommendationsControllerGrpc;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.practicum.ewm.service.RecommendationService;


@GrpcService
@RequiredArgsConstructor
public class RecommendationController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        recommendationService.getSimilarEvents(request, responseObserver);
    }

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        recommendationService.getRecommendationsForUser(request, responseObserver);
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        recommendationService.getInteractionsCount(request, responseObserver);
    }
}
