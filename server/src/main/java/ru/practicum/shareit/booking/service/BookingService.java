package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, BookingCreateDto bookingCreateDto);
    BookingDto approve(Long userId, Long bookingId, Boolean approved);
    BookingDto getBooking(Long userId, Long bookingId);
    List<BookingDto> getBookings(Long userId, String state);
    List<BookingDto> getBookingsByOwner(Long userId, String state);
}