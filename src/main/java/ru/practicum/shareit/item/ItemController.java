package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService service;

    private static final String USER_HEADER = "X-Sharer-User-Id";

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
    public ItemDto get(@PathVariable Long itemId,
                       @RequestHeader(USER_HEADER) Long userId) {
        return service.get(itemId, userId);
    }

    @GetMapping
    public List<ItemDto> getAll(@RequestHeader(USER_HEADER) Long userId) {
        return service.getAll(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        return service.search(text);
    }
}