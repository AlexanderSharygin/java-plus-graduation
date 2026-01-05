package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.feign_clients.RequestClient;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.service.RequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class FeignController implements RequestClient {

    private final RequestService requestService;

    @Override
    public List<RequestDto> getConfirmedRequestsForEvent(@RequestBody List<Event> events) {
       return  requestService.getConfirmedRequestsForEvent(events);
    }
}