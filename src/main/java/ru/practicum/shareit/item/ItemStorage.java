package ru.practicum.shareit.item;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ItemStorage {
    private final Map<Long, Item> items = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(idGen.getAndIncrement());
        }
        items.put(item.getId(), item);
        return item;
    }

    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<Item> findAllByOwner(Long ownerId) {
        return items.values().stream()
                .filter(i -> i.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    public List<Item> search(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String lower = text.toLowerCase();
        return items.values().stream()
                .filter(i -> Boolean.TRUE.equals(i.getAvailable()))
                .filter(i -> (i.getName() != null && i.getName().toLowerCase().contains(lower))
                        || (i.getDescription() != null && i.getDescription().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }
}