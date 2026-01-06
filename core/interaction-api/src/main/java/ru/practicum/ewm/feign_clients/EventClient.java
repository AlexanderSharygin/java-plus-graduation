package ru.practicum.ewm.feign_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.dto.event.EventDto;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("event/{eventId}")
    EventDto getEventById(@PathVariable Long eventId);
}
