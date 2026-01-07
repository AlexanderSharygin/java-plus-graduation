package ru.practicum.ewm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDto> getAll(List<Long> usersId, Pageable pageable) {
        if (usersId == null) {
            return userRepository.findAll(pageable).stream()
                    .map(UserMapper::toUserDtoFromUser).toList();
        } else {
            return userRepository.findAllByIdIn(usersId, pageable).stream()
                    .map(UserMapper::toUserDtoFromUser).toList();
        }
    }

    public List<UserDto> getAll(List<Long> usersId) {
        return userRepository.findAllByIdIn(usersId).stream()
                .map(UserMapper::toUserDtoFromUser).toList();
    }


    public UserDto getById(long userId) {
        return UserMapper.toUserDtoFromUser(getUserIfExist(userId));
    }

    public UserDto create(UserDto userDto) {
        Optional<User> userWithSameName = userRepository.findByName(userDto.getName());
        if (userWithSameName.isPresent()) {
            throw new ConflictException("User " + userDto.getName() + " уже существует!");
        }

        return UserMapper.toUserDtoFromUser(userRepository.save(UserMapper.toUserFromUserDto(userDto)));
    }

    public void delete(long userId) {
        userRepository.delete(getUserIfExist(userId));
    }

    public User getUserIfExist(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User c id" + userId + " не существует!"));
    }
}