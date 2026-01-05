package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.service.UserService;


@RestController
@Validated
@Slf4j
@RequiredArgsConstructor
public class UserFeignController implements UserClient {
    private final UserService service;

    @Override
    public UserDto getUserById(Long userId) {
        return service.getById(userId);
    }
}
