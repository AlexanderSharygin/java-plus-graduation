package ru.practicum.ewm.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.grpc.stats.action.UserActionProto;
import ru.practicum.ewm.grpc.stats.collector.UserActionControllerGrpc;
import ru.practicum.ewm.handler.UserActionHandler;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionHandler handler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получено действие пользователя {} для события {}.", request.getActionType(), request.getEventId());
        try {
            handler.collectUserAction(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Произошла ошибка при обработке действие пользователя {} для события {}. Ошибка={}",
                    request.getActionType(), request.getEventId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
