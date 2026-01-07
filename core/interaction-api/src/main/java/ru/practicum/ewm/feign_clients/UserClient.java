package ru.practicum.ewm.feign_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("user/{userId}")
    UserDto getUserById(@PathVariable Long userId);

    @GetMapping("user")
    List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> usersIds);
}
