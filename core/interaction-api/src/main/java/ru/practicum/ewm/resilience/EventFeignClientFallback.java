package ru.practicum.ewm.resilience;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.event.EventCommentDto;
import ru.practicum.ewm.dto.event.EventDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.ServiceUnavailableException;
import ru.practicum.ewm.feign_clients.EventClient;
import ru.practicum.ewm.feign_clients.UserClient;

import java.util.List;


@Component
public class EventFeignClientFallback implements EventClient {

    @Override
    public EventDto getEventById(Long eventId) {
        throw new ServiceUnavailableException("Events-service временно недоступен");
    }

    @Override
    public List<EventCommentDto> getEventsDtoForComments(List<Long> eventIds) {
        throw new ServiceUnavailableException("Events-service временно недоступен");
    }
}