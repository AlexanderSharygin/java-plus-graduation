package ru.practicum.ewm.feign_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.event.EventCommentDto;
import ru.practicum.ewm.dto.event.EventDto;

import java.util.List;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("event/{eventId}")
    EventDto getEventById(@PathVariable Long eventId);

    @GetMapping("event")
    List<EventCommentDto> getEventsDtoForComments(@RequestParam(value = "ids", required = false) List<Long> eventIds);
}
