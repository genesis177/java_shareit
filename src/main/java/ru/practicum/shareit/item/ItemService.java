package ru.practicum.shareit.item;

import java.util.List;

public interface ItemService {
    ItemDto create(ItemDto itemDto, Long ownerId);

    ItemDto update(Long itemId, ItemDto itemDto, Long ownerId);

    ItemDtoWithBookings get(Long itemId, Long userId);

    List<ItemDtoWithBookings> getAllByOwner(Long ownerId);

    List<ItemDto> search(String text);

    CommentDto createComment(Long itemId, Long userId, CommentDto commentDto);

    List<ItemDto> getAll(Long userId);
}