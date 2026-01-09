package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.service.UserService;

import java.util.List;


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

    @Override
    public List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> usersIds) {
        return service.getAll(usersIds);
    }
}
