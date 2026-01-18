package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminDto;
import ru.practicum.ewm.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
public class EventAdminController {
    private final EventService eventService;


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

        return eventService.getAll(users, states, categories, rangeStart, rangeEnd, PageRequest.of(from, size));
    }

    @PatchMapping("/{eventId}")
    public EventDto updateEvent(@PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminDto eventDto) {

        return eventService.updateByAdmin(eventId, eventDto);
    }

}
