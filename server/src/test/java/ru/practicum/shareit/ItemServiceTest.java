package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User user;
    private Item item;
    private ItemDto itemDto;
    private Comment comment;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .name("Owner")
                .email("owner@example.com")
                .build();

        user = User.builder()
                .id(2L)
                .name("User")
                .email("user@example.com")
                .build();

        item = Item.builder()
                .id(1L)
                .name("Item")
                .description("Description")
                .available(true)
                .owner(owner)
                .build();

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Item")
                .description("Description")
                .available(true)
                .build();

        comment = Comment.builder()
                .id(1L)
                .text("Comment text")
                .item(item)
                .author(user)
                .created(LocalDateTime.now())
                .build();

        commentDto = new CommentDto();
        commentDto.setId(1L);
        commentDto.setText("Comment text");
        commentDto.setAuthorName("User");
        commentDto.setCreated(LocalDateTime.now());
    }

    @Test
    void create_ValidItem_ReturnsItemDto() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.create(owner.getId(), itemDto);

        assertNotNull(result);
        assertEquals(item.getName(), result.getName());
        assertEquals(item.getDescription(), result.getDescription());
        assertEquals(item.getAvailable(), result.getAvailable());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void create_NonExistingUser_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.create(999L, itemDto));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void update_ValidUpdate_ReturnsUpdatedItemDto() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated Description");

        ItemDto result = itemService.update(owner.getId(), item.getId(), updateDto);

        assertNotNull(result);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void update_NonOwner_ThrowsNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Name");

        assertThrows(NotFoundException.class, () -> itemService.update(999L, item.getId(), updateDto));
    }

    @Test
    void get_ValidItem_ReturnsItemDto() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(commentRepository.findByItemIdOrderByCreatedDesc(anyLong())).thenReturn(Collections.emptyList());

        ItemDto result = itemService.get(owner.getId(), item.getId());

        assertNotNull(result);
        assertEquals(item.getName(), result.getName());
    }

    @Test
    void getAll_ValidUser_ReturnsItemList() {
        when(itemRepository.findByOwnerIdOrderByIdAsc(anyLong())).thenReturn(List.of(item));
        // Добавляем моки для бронирований и комментариев, которые используются в getAll
        when(bookingRepository.findLastBookingForItem(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(bookingRepository.findNextBookingForItem(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(commentRepository.findByItemIdOrderByCreatedDesc(anyLong())).thenReturn(Collections.emptyList());

        List<ItemDto> result = itemService.getAll(owner.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void search_EmptyText_ReturnsEmptyList() {
        List<ItemDto> result = itemService.search("");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void search_ValidText_ReturnsItemList() {
        when(itemRepository.searchByText(anyString())).thenReturn(List.of(item));

        List<ItemDto> result = itemService.search("item");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void createComment_ValidComment_ReturnsCommentDto() {
        when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
        when(itemRepository.findById(eq(item.getId()))).thenReturn(Optional.of(item));
        // Исправляем мок - используем правильный метод из реализации сервиса
        when(bookingRepository.existsApprovedBookingByUserAndItem(
                eq(user.getId()),
                eq(item.getId()),
                any(LocalDateTime.class)))
                .thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto inputDto = new CommentDto();
        inputDto.setText("Comment text");

        CommentDto result = itemService.createComment(user.getId(), item.getId(), inputDto);

        assertNotNull(result);
        assertEquals(comment.getText(), result.getText());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_NoBooking_ThrowsBadRequestException() {
        when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
        when(itemRepository.findById(eq(item.getId()))).thenReturn(Optional.of(item));
        // Исправляем мок - используем правильный метод из реализации сервиса
        when(bookingRepository.existsApprovedBookingByUserAndItem(
                eq(user.getId()),
                eq(item.getId()),
                any(LocalDateTime.class)))
                .thenReturn(false);

        CommentDto inputDto = new CommentDto();
        inputDto.setText("Comment text");

        assertThrows(BadRequestException.class,
                () -> itemService.createComment(user.getId(), item.getId(), inputDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }
}