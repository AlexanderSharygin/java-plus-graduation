package ru.practicum.ewm.main.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.main.event.dto.EventDto;
import ru.practicum.ewm.main.event.dto.UpdateEventAdminDto;
import ru.practicum.ewm.main.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/admin/events")
public class EventAdminController {
    private final StatsClient statClient;
    private final EventService eventService;

    @Autowired
    public EventAdminController(StatsClient statClient, EventService eventService) {
        this.statClient = statClient;
        this.eventService = eventService;
    }

    //events
    @GetMapping
    public List<EventDto> getAll(@RequestParam(value = "users", required = false) List<Long> users,
                                 @RequestParam(value = "states", required = false) List<String> states,
                                 @RequestParam(value = "categories", required = false) List<Long> categories,
                                 @RequestParam(value = "rangeStart", required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                 @RequestParam(value = "rangeEnd", required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                 @PositiveOrZero @RequestParam(value = "from", defaultValue = "0") int from,
                                 @Positive @RequestParam(value = "size", defaultValue = "10") int size,
                                 HttpServletRequest request) {
        statClient.create(new HitDto(request.getRemoteAddr(), "ewm-main", "/events", LocalDateTime.now()));

        return eventService.getAll(users, states, categories, rangeStart, rangeEnd, PageRequest.of(from, size));
    }

    @PatchMapping("/{eventId}")
    public EventDto updateEvent(@PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminDto eventDto) {

        return eventService.updateByAdmin(eventId, eventDto);
    }

}
