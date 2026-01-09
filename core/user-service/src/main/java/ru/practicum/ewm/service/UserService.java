package ru.practicum.ewm.service;


import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getAll(List<Long> usersId, Pageable pageable);

    List<UserDto> getAll(List<Long> usersId);

    UserDto getById(long userId);

    UserDto create(UserDto userDto);

    void delete(long userId);


}
