package ru.practicum.ewm.main.request.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.request.dto.RequestDto;
import ru.practicum.ewm.main.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class PrivateController {

    private final RequestService requestService;

    @Autowired
    public PrivateController(RequestService requestService) {
        this.requestService = requestService;
    }

    //requests
    @GetMapping("/{userId}/requests")
    public List<RequestDto> getUserEvents(@PathVariable Long userId) {

        return requestService.getByUserId(userId);
    }

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto createRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public RequestDto cancelByUser(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequestByUser(userId, requestId);
    }
}