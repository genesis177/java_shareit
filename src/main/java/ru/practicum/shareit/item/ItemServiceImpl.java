package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemStorage storage;
    private final UserStorage userStorage;

    @Override
    public ItemDto create(ItemDto itemDto, Long ownerId) {
        validate(itemDto, true);
        if (userStorage.findById(ownerId).isEmpty()) {
            throw new NotFoundException("Пользователь не найден");
        }
        Item item = ItemMapper.toItem(itemDto, ownerId);
        item.setId(null);
        return ItemMapper.toDto(storage.save(item));
    }

    @Override
    public ItemDto update(Long itemId, ItemDto itemDto, Long ownerId) {
        Item item = storage.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Предмет не найден"));
        if (!item.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("Только владелец может обновить предмет");
        }
        if (itemDto.getName() != null) {
            if (itemDto.getName().isBlank()) throw new ValidationException("Имя не может быть пустым");
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            if (itemDto.getDescription().isBlank()) throw new ValidationException("Описание не может быть пустым");
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        return ItemMapper.toDto(storage.save(item));
    }

    @Override
    public ItemDto get(Long itemId, Long userId) {
        return ItemMapper.toDto(storage.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Предмет не найден")));
    }

    @Override
    public List<ItemDto> getAll(Long ownerId) {
        return storage.findAllByOwner(ownerId).stream()
                .map(ItemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        return storage.search(text).stream()
                .map(ItemMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validate(ItemDto itemDto, boolean isCreate) {
        if (isCreate && (itemDto.getName() == null || itemDto.getName().isBlank())) {
            throw new ValidationException("Необходимо ввести имя");
        }
        if (isCreate && (itemDto.getDescription() == null || itemDto.getDescription().isBlank())) {
            throw new ValidationException("Необходимо ввести описание");
        }
        if (isCreate && itemDto.getAvailable() == null) {
            throw new ValidationException("Доступность обязательна");
        }
    }
}
