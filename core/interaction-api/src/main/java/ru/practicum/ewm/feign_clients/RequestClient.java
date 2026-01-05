package ru.practicum.ewm.feign_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.model.event.Event;

import java.util.List;

@FeignClient(name = "request-service")
public interface RequestClient {

    @PostMapping("/request/all")
    List<RequestDto> getConfirmedRequestsForEvent(@RequestBody List<Event> events);
}