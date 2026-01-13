package ru.practicum.ewm.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.practicum.ewm.service.RecommendationServiceImpl;

@RequiredArgsConstructor
@Slf4j
@GrpcService
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationServiceImpl service;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        service.getSimilarEvents(request, responseObserver);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        service.getInteractionsCount(request, responseObserver);
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        service.getRecommendationsForUser(request, responseObserver);
    }
}
