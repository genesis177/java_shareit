package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ConflictException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@[\\w-]+\\.[\\w-]{2,}$");
    private final UserStorage storage;


    @Override
    public UserDto create(UserDto userDto) {
        validate(userDto, true);
        if (storage.findByEmail(userDto.getEmail()).isPresent()) {
            throw new ConflictException("Email уже существует");
        }
        User user = UserMapper.toUser(userDto);
        user.setId(null);
        return UserMapper.toDto(storage.save(user));
    }

    @Override
    public UserDto update(Long id, UserDto userDto) {
        User user = storage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (userDto.getEmail() != null) {
            if (!EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
                throw new ValidationException("Неподходящий email");
            }
            if (!user.getEmail().equalsIgnoreCase(userDto.getEmail())
                    && storage.findByEmail(userDto.getEmail()).isPresent()) {
                throw new ConflictException("Email уже существует");
            }
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        return UserMapper.toDto(storage.save(user));
    }

    @Override
    public UserDto get(Long id) {
        return UserMapper.toDto(storage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден")));
    }

    @Override
    public List<UserDto> getAll() {
        return storage.findAll().stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!storage.findById(id).isPresent()) {
            throw new NotFoundException("Пользователь не найден");
        }
        storage.delete(id);
    }

    private void validate(UserDto userDto, boolean isCreate) {
        if (isCreate && (userDto.getEmail() == null || userDto.getEmail().isBlank())) {
            throw new ValidationException("Email необходим");
        }
        if (userDto.getEmail() != null && !EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
            throw new ValidationException("Неподходящий email");
        }
    }
}