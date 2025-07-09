package ru.practicum.shareit.item;

import ru.practicum.shareit.booking.BookingShortDto;
import ru.practicum.shareit.user.User;

import java.util.Collections;
import java.util.List;

public class ItemMapper {
    public static ItemDto toDto(Item item) {
        if (item == null) return null;
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .build();
    }

    public static Item toItem(ItemDto dto, User owner) {
        if (dto == null) return null;
        return Item.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .available(dto.getAvailable())
                .owner(owner)
                .build();
    }

    public static ItemDtoWithBookings toDtoWithBookings(Item item, BookingShortDto lastBooking,
                                                        BookingShortDto nextBooking, List<CommentDto> comments) {
        if (item == null) return null;
        return ItemDtoWithBookings.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .lastBooking(lastBooking)
                .nextBooking(nextBooking)
                .comments(comments != null ? comments : Collections.emptyList())
                .build();
    }
}