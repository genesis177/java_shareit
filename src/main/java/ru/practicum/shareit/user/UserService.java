package ru.practicum.shareit.user;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    UserDto update(Long id, UserDto userDto);

    UserDto get(Long id);

    List<UserDto> getAll();

    void delete(Long id);
}