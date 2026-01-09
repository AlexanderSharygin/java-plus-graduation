package ru.practicum.ewm.resilience;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.ServiceUnavailableException;
import ru.practicum.ewm.feign_clients.UserClient;


import java.util.List;


@Component
public class UserFeignClientFallback implements UserClient {

    @Override
    public UserDto getUserById(Long userId) {
        throw new ServiceUnavailableException("User-service временно недоступен");
    }

    @Override
    public List<UserDto> getUsers(List<Long> usersIds) {
        throw new ServiceUnavailableException("User-service временно недоступен");
    }
}