package ru.practicum.shareit.item;

import java.util.List;

public interface ItemService {
    ItemDto create(ItemDto itemDto, Long ownerId);

    ItemDto update(Long itemId, ItemDto itemDto, Long ownerId);

    ItemDto get(Long itemId, Long userId);

    List<ItemDto> getAll(Long ownerId);

    List<ItemDto> search(String text);
}