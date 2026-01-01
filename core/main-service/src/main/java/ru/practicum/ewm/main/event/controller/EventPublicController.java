package ru.practicum.ewm.main.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.main.event.dto.EventDto;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/events")
public class EventPublicController {

    private final EventService eventService;
    private final StatsClient statClient;


    @Autowired
    public EventPublicController(EventService eventService, StatsClient statClient) {
        this.eventService = eventService;
        this.statClient = statClient;
    }

    @GetMapping("/{id}")
    public EventDto getEvent(@PathVariable Long id,
                             HttpServletRequest request) {
        statClient.create(new HitDto(request.getRemoteAddr(), "ewm-main", request.getRequestURI(),
                LocalDateTime.now()));

        return eventService.getById(id);
    }

    @GetMapping
    public List<EventShortDto> getAllEvents(@RequestParam(value = "text", required = false) @Size(min = 3) String text,
                                            @RequestParam(value = "categories", required = false) List<Long> categories,
                                            @RequestParam(value = "paid", required = false) Boolean paid,
                                            @RequestParam(value = "rangeStart", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                            @RequestParam(value = "rangeEnd", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                            @RequestParam(value = "onlyAvailable", defaultValue = "false")
                                            boolean onlyAvailable,
                                            @RequestParam(value = "sort", required = false) String sort,
                                            @PositiveOrZero @RequestParam(value = "from", defaultValue = "0") int from,
                                            @Positive @RequestParam(value = "size", defaultValue = "10") int size,
                                            HttpServletRequest request) {
        statClient.create(new HitDto(request.getRemoteAddr(), "ewm-main", request.getRequestURI(),
                LocalDateTime.now()));

        return eventService.getAllShort(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }
}