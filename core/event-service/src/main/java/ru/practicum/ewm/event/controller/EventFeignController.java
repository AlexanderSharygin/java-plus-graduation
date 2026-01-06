package ru.practicum.ewm.event.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.event.EventDto;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.feign_clients.EventClient;

@RestController
@Validated
@Slf4j
@RequiredArgsConstructor
public class EventFeignController implements EventClient {

    private final EventService service;

    @Override
    public EventDto getEventById(@PathVariable Long eventId) {
        return service.getById(eventId);
    }
}
