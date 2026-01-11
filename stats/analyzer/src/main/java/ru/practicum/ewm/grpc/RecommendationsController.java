package ru.practicum.ewm.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.grpc.stats.recommendations.InteractionsCountRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.grpc.stats.recommendations.SimilarEventsRequestProto;
import ru.practicum.ewm.grpc.stats.recommendations.UserPredictionsRequestProto;
import ru.practicum.ewm.service.EventSimilarityService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final EventSimilarityService service;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос getSimilarEvents для события {}.", request.getEventId());
        try {
            List<RecommendedEventProto> response = service.getSimilarEvents(request);
            response.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Произошла ошибка при обработке запрос getSimilarEvents для события {}. Ошибка={}", request.getEventId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос getInteractionsCount для событий {}.", request.getEventIdList());
        try {
            List<RecommendedEventProto> response = service.getInteractionsCount(request);
            response.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Произошла ошибка при обработке запрос getInteractionsCount для событий {}. Ошибка={}",
                    request.getEventIdList(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос getRecommendationsForUser для пользователя {}.", request.getUserId());
        try {
            List<RecommendedEventProto> response = service.getRecommendationsForUser(request);
            response.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Произошла ошибка при обработке запрос getRecommendationsForUser для пользователя {}. Ошибка={}",
                    request.getUserId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
