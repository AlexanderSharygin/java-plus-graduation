package ru.practicum.ewm.main.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.main.event.dto.CreateNewEventDto;
import ru.practicum.ewm.main.event.dto.EventDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.main.request.dto.RequestDto;
import ru.practicum.ewm.main.request.dto.RequestStatusUpdateRequest;
import ru.practicum.ewm.main.request.dto.RequestStatusUpdateResponse;
import ru.practicum.ewm.main.request.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class EventPrivateController {

    private final EventService eventService;
    private final RequestService requestService;
    private final StatsClient statClient;

    @Autowired
    public EventPrivateController(EventService eventService, RequestService requestService, StatsClient statClient) {
        this.eventService = eventService;
        this.requestService = requestService;
        this.statClient = statClient;
    }

    @GetMapping("/{userId}/events")
    public List<EventShortDto> getUserEvents(@PathVariable Long userId,
                                             @PositiveOrZero @RequestParam(value = "from", defaultValue = "0") int from,
                                             @Positive @RequestParam(value = "size", defaultValue = "10") int size,
                                             HttpServletRequest request) {
        Pageable paging = PageRequest.of(from, size);
        statClient.create(new HitDto(request.getRemoteAddr(), "ewm-main", "/events", LocalDateTime.now()));

        return eventService.getByUserId(userId, paging);
    }

    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto createEvent(@RequestBody @Valid CreateNewEventDto newEventDto, @PathVariable Long userId) {

        return eventService.create(newEventDto, userId);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public EventDto getUserEvents(@PathVariable Long userId, @PathVariable Long eventId, HttpServletRequest request) {
        statClient.create(new HitDto(request.getRemoteAddr(), "ewm-main", "/events", LocalDateTime.now()));

        return eventService.getEventByUserId(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    public EventDto updateEvent(@RequestBody @Valid UpdateEventUserRequest eventDto,
                                @PathVariable Long userId, @PathVariable Long eventId, HttpServletRequest request) {
        statClient.create(new HitDto(request.getRemoteAddr(), "ewm-main", "/events/" + eventId,
                LocalDateTime.now()));

        return eventService.updateByUser(eventDto, userId, eventId);
    }

    @GetMapping("/{userId}/events/{eventId}/requests")
    public List<RequestDto> getEventRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    public RequestStatusUpdateResponse updateRequest(@PathVariable Long userId, @PathVariable Long eventId,
                                                     @RequestBody RequestStatusUpdateRequest request) {
        return requestService.updateRequest(userId, eventId, request);
    }
}