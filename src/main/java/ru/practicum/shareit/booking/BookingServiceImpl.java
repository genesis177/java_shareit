package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private static final Sort SORT_BY_START_DESC = Sort.by(Sort.Direction.DESC, "start");

    @Override
    public BookingDto create(BookingCreateDto bookingDto, Long userId) {
        validate(bookingDto);

        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!item.getAvailable()) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }

        Booking booking = BookingMapper.toBooking(bookingDto, item, booker, BookingStatus.WAITING);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(Long bookingId, Long ownerId, Boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ValidationException("Только владелец вещи может подтвердить бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        if (!booking.getBooker().getId().equals(userId) && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Доступ запрещен");
        }

        return BookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getAllByBooker(Long bookerId, String state) {
        if (!userRepository.existsById(bookerId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state) {
            case "ALL":
                bookings = bookingRepository.findByBookerId(bookerId, SORT_BY_START_DESC);
                break;
            case "CURRENT":
                bookings = bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(
                        bookerId, now, now, SORT_BY_START_DESC);
                break;
            case "PAST":
                bookings = bookingRepository.findByBookerIdAndEndBefore(
                        bookerId, now, SORT_BY_START_DESC);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByBookerIdAndStartAfter(
                        bookerId, now, SORT_BY_START_DESC);
                break;
            case "WAITING":
                bookings = bookingRepository.findByBookerIdAndStatus(
                        bookerId, BookingStatus.WAITING, SORT_BY_START_DESC);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByBookerIdAndStatus(
                        bookerId, BookingStatus.REJECTED, SORT_BY_START_DESC);
                break;
            default:
                throw new ValidationException("Неизвестный статус: " + state);
        }

        return bookings.stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getAllByOwner(Long ownerId, String state) {
        if (!userRepository.existsById(ownerId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state) {
            case "ALL":
                bookings = bookingRepository.findByItemOwnerId(ownerId, SORT_BY_START_DESC);
                break;
            case "CURRENT":
                bookings = bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(
                        ownerId, now, now, SORT_BY_START_DESC);
                break;
            case "PAST":
                bookings = bookingRepository.findByItemOwnerIdAndEndBefore(
                        ownerId, now, SORT_BY_START_DESC);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByItemOwnerIdAndStartAfter(
                        ownerId, now, SORT_BY_START_DESC);
                break;
            case "WAITING":
                bookings = bookingRepository.findByItemOwnerIdAndStatus(
                        ownerId, BookingStatus.WAITING, SORT_BY_START_DESC);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByItemOwnerIdAndStatus(
                        ownerId, BookingStatus.REJECTED, SORT_BY_START_DESC);
                break;
            default:
                throw new ValidationException("Неизвестный статус: " + state);
        }

        return bookings.stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validate(BookingCreateDto bookingDto) {
        if (bookingDto.getStart() == null) {
            throw new ValidationException("Дата начала бронирования не может быть пустой");
        }
        if (bookingDto.getEnd() == null) {
            throw new ValidationException("Дата окончания бронирования не может быть пустой");
        }
        if (bookingDto.getStart().isAfter(bookingDto.getEnd())) {
            throw new ValidationException("Дата начала бронирования не может быть позже даты окончания");
        }
        if (bookingDto.getStart().equals(bookingDto.getEnd())) {
            throw new ValidationException("Дата начала бронирования не может совпадать с датой окончания");
        }
        if (bookingDto.getStart().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Дата начала бронирования не может быть в прошлом");
        }
    }
}