package ru.practicum.shareit.item;

import java.util.List;

public interface ItemService {
    /**
     * Создать предмет.
     */
    ItemDto create(ItemDto itemDto, Long ownerId);

    /**
     * Обновить предмет.
     */
    ItemDto update(Long itemId, ItemDto itemDto, Long ownerId);

    /**
     * Получить предмет по id.
     */
    ItemDto get(Long itemId, Long userId);

    /**
     * Получить все предметы пользователя.
     */
    List<ItemDto> getAll(Long ownerId);

    /**
     * Поиск предметов по тексту.
     */
    List<ItemDto> search(String text);
}