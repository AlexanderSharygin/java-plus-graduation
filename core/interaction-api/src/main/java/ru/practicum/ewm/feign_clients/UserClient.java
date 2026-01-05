package ru.practicum.ewm.feign_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.dto.user.UserDto;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("user/{userId}")
    UserDto getUserById(@PathVariable Long userId);
}
