package ru.practicum.ewm.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.grpc.stats.analyzer.RecommendationsControllerGrpc;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.practicum.ewm.service.EventSimilarityService;


import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final EventSimilarityService service;



    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<RecommendedEventProto> response = service.getSimilarEvents(request);
        response.stream().forEach(x -> responseObserver.onNext(x));
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<RecommendedEventProto> response = service.getInteractionsCount(request);
        response.stream().forEach(x -> responseObserver.onNext(x));
        responseObserver.onCompleted();
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<RecommendedEventProto> response = service.getRecommendationsForUser(request);
        response.stream().forEach(x -> responseObserver.onNext(x));
        responseObserver.onCompleted();
    }
}
