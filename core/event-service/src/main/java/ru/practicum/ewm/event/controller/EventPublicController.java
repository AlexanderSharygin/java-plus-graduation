package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.CollectorClient;
import ru.practicum.ewm.dto.event.EventDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
public class EventPublicController {

    private final EventService eventService;
    private final CollectorClient collectorClient;


    @GetMapping("/{id}")
    public EventDto getEvent(@RequestHeader("X-EWM-USER-ID") long userId, @PathVariable Long id, HttpServletRequest request) {
        collectorClient.sendEventView(userId, id);
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

        return eventService.getAllShort(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/recommendations")
    public List<EventDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") long userId, @RequestParam Integer maxResults) {
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId, @RequestHeader("X-EWM-USER-ID") long userId) {
        eventService.likeEvent(eventId, userId);
        collectorClient.sendEventLike(userId, eventId);
    }
}