package ru.practicum.ewm.resilience;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.exception.ServiceUnavailableException;
import ru.practicum.ewm.feign_clients.RequestClient;
import ru.practicum.ewm.model.event.Event;

import java.util.List;


@Component
public class RequestFeignClientFallback implements RequestClient {


    @Override
    public List<RequestDto> getConfirmedRequestsForEvent(List<Event> events) {
        throw new ServiceUnavailableException("Request-service временно недоступен");
    }
}