package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private static final String USER_HEADER = "X-Sharer-User-Id";
    private final ItemService service;

    @PostMapping
    public ItemDto create(@RequestBody ItemDto itemDto,
                          @RequestHeader(USER_HEADER) Long userId) {
        return service.create(itemDto, userId);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@PathVariable Long itemId,
                          @RequestBody ItemDto itemDto,
                          @RequestHeader(USER_HEADER) Long userId) {
        return service.update(itemId, itemDto, userId);
    }

    @GetMapping("/{itemId}")
    public ItemDtoWithBookings get(@PathVariable Long itemId,
                                   @RequestHeader(USER_HEADER) Long userId) {
        return service.get(itemId, userId);
    }

    @GetMapping
    public List<ItemDtoWithBookings> getAll(@RequestHeader(USER_HEADER) Long userId) {
        return service.getAllByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        return service.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@PathVariable Long itemId,
                                 @RequestBody CommentDto commentDto,
                                 @RequestHeader(USER_HEADER) Long userId) {
        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Текст комментария обязателен");
        }
        return service.createComment(itemId, userId, commentDto);
    }
}