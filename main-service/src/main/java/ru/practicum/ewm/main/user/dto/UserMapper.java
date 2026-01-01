package ru.practicum.ewm.main.user.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.user.model.User;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static UserDto toUserDtoFromUser(User user) {
        UserDto userDto = new UserDto(user.getEmail(), -1, user.getName());
        if (user.getId() != null) {
            userDto.setId(user.getId());
        }

        return userDto;
    }

    public static UserShortDto fromUserToUserShortDto(User user) {
        UserShortDto userShortDto = new UserShortDto(-1, user.getName());
        if (user.getId() != null) {
            userShortDto.setId(user.getId());
        }

        return userShortDto;
    }

    public static User toUserFromUserDto(UserDto userDto) {
        return new User(userDto.getId(), userDto.getName(), userDto.getEmail());
    }
}