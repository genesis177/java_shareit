package ru.practicum.shareit.booking;

import java.util.List;

public interface BookingService {
    BookingDto create(BookingCreateDto bookingDto, Long userId);

    BookingDto approve(Long bookingId, Long ownerId, Boolean approved);

    BookingDto getById(Long bookingId, Long userId);

    List<BookingDto> getAllByBooker(Long bookerId, String state);

    List<BookingDto> getAllByOwner(Long ownerId, String state);
}