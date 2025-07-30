package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto create(Long userId, ItemDto itemDto);
    ItemDto update(Long userId, Long itemId, ItemDto itemDto);
    ItemDto get(Long userId, Long itemId);
    List<ItemDto> getAll(Long userId);
    List<ItemDto> search(String text);
    CommentDto createComment(Long userId, Long itemId, CommentDto commentDto);
}