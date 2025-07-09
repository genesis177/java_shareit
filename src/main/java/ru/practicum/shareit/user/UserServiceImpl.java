package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@[\\w-]+.[\\w-]{2,}$");
    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        validate(userDto, true);
        if (userRepository.findByEmailIgnoreCase(userDto.getEmail()).isPresent()) {
            throw new ConflictException("Email уже существует");
        }
        User user = UserMapper.toUser(userDto);
        user.setId(null);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (userDto.getEmail() != null) {
            if (!EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
                throw new ValidationException("Неподходящий email");
            }
            if (!user.getEmail().equalsIgnoreCase(userDto.getEmail()) &&
                    userRepository.findByEmailIgnoreCase(userDto.getEmail()).isPresent()) {
                throw new ConflictException("Email уже существует");
            }
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return UserMapper.toDto(userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
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