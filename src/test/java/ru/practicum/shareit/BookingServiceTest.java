package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.*;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private BookingCreateDto bookingCreateDto;
    private BookingDto.BookerDto bookerDto;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .name("Owner")
                .email("owner@example.com")
                .build();

        booker = User.builder()
                .id(2L)
                .name("Booker")
                .email("booker@example.com")
                .build();

        item = Item.builder()
                .id(1L)
                .name("Item")
                .description("Description")
                .available(true)
                .owner(owner)
                .build();

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        booking = Booking.builder()
                .id(1L)
                .start(start)
                .end(end)
                .item(item)
                .booker(booker)
                .status(BookingStatus.WAITING)
                .build();

        bookingCreateDto = BookingCreateDto.builder()
                .itemId(1L)
                .start(start)
                .end(end)
                .build();

        BookingDto bookingDto = BookingDto.builder()
                .id(1L)
                .start(start)
                .end(end)
                .item(new BookingDto.ItemDto(1L, "Item"))
                .booker(new BookingDto.BookerDto(2L, "Booker"))
                .status(BookingStatus.WAITING)
                .build();
    }

    @Test
    void create_ValidBooking_ReturnsBookingDto() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.create(bookingCreateDto, booker.getId());

        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
        assertEquals(booking.getStart(), result.getStart());
        assertEquals(booking.getEnd(), result.getEnd());
        assertEquals(booking.getItem().getId(), result.getItem().getId());
        assertEquals(booking.getBooker().getId(), result.getBooker().getId());
        assertEquals(booking.getStatus(), result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_NonExistingUser_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(bookingCreateDto, 999L));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void create_NonExistingItem_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(bookingCreateDto, booker.getId()));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void create_UnavailableItem_ThrowsValidationException() {
        item.setAvailable(false);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class, () -> bookingService.create(bookingCreateDto, booker.getId()));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void create_OwnerBookingOwnItem_ThrowingOwnItem_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class, () -> bookingService.create(bookingCreateDto, owner.getId()));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approve_ValidApproval_ReturnsApprovedBookingDto() {
        Booking approvedBooking = Booking.builder()
                .id(1L)
                .start(booking.getStart())
                .end(booking.getEnd())
                .item(item)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build();

        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(approvedBooking);

        BookingDto result = bookingService.approve(1L, owner.getId(), true);

        assertNotNull(result);
        assertEquals(approvedBooking.getId(), result.getId());
        assertEquals(approvedBooking.getStatus(), result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void approve_NonExistingBooking_ThrowsNotFoundException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.approve(999L, owner.getId(), true));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approve_NotOwner_ThrowsNotFoundException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingService.approve(1L, booker.getId(), true));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approve_AlreadyProcessed_ThrowsValidationException() {
        booking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () -> bookingService.approve(1L, owner.getId(), true));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void getById_ExistingBooking_ReturnsBookingDto() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(1L, booker.getId());

        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
        assertEquals(booking.getStart(), result.getStart());
        assertEquals(booking.getEnd(), result.getEnd());
    }

    @Test
    void getById_NonExistingBooking_ThrowsNotFoundException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.getById(999L, booker.getId()));
    }

    @Test
    void getById_NotOwnerOrBooker_ThrowsNotFoundException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingService.getById(1L, 999L));
    }

    @Test
    void getAllByBooker_WithStateAll_ReturnsListOfBookingDto() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        when(bookingRepository.findByBookerId(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "ALL");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(booking.getId(), result.get(0).getId());
    }

    @Test
    void getAllByBooker_NonExistingUser_ThrowsNotFoundException() {
        when(userRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> bookingService.getAllByBooker(999L, "ALL"));
    }

    @Test
    void getAllByBooker_InvalidState_ThrowsValidationException() {
        when(userRepository.existsById(anyLong())).thenReturn(true);

        assertThrows(ValidationException.class, () -> bookingService.getAllByBooker(booker.getId(), "INVALID_STATE"));
    }

    @Test
    void getAllByOwner_WithStateAll_ReturnsListOfBookingDto() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        when(bookingRepository.findByItemOwnerId(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "ALL");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(booking.getId(), result.get(0).getId());
    }

    @Test
    void getAllByOwner_NonExistingUser_ThrowsNotFoundException() {
        when(userRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> bookingService.getAllByOwner(999L, "ALL"));
    }

    @Test
    void getAllByOwner_InvalidState_ThrowsValidationException() {
        when(userRepository.existsById(anyLong())).thenReturn(true);

        assertThrows(ValidationException.class, () -> bookingService.getAllByOwner(owner.getId(), "INVALID_STATE"));
    }
}