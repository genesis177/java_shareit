package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.*;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

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

    private ItemService itemService;
    private User owner;
    private Item item;
    private ItemDto itemDto;
    private Comment comment;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        // Создаем анонимный класс, наследующийся от ItemServiceImpl
        itemService = new ItemServiceImpl(itemRepository, userRepository, bookingRepository, commentRepository) {
            @Override
            public List<ItemDto> getAll(Long userId) {
                return Collections.emptyList();
            }
        };

        // Инициализация тестовых данных
        owner = User.builder()
                .id(1L)
                .name("Owner")
                .email("owner@example.com")
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

        User commentAuthor = User.builder()
                .id(2L)
                .name("Author")
                .email("author@example.com")
                .build();

        comment = Comment.builder()
                .id(1L)
                .text("Comment text")
                .item(item)
                .author(commentAuthor)
                .created(LocalDateTime.now())
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("Comment text")
                .authorName("Author")
                .created(comment.getCreated())
                .build();
    }

    @Test
    void create_ValidItem_ReturnsItemDto() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.create(itemDto, owner.getId());

        assertNotNull(result);
        assertEquals(itemDto.getId(), result.getId());
        assertEquals(itemDto.getName(), result.getName());
        assertEquals(itemDto.getDescription(), result.getDescription());
        assertEquals(itemDto.getAvailable(), result.getAvailable());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void create_NonExistingOwner_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.create(itemDto, 999L));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void update_ExistingItem_ReturnsUpdatedItemDto() {
        ItemDto updateDto = ItemDto.builder()
                .name("Updated Item")
                .description("Updated Description")
                .available(false)
                .build();

        Item updatedItem = Item.builder()
                .id(1L)
                .name("Updated Item")
                .description("Updated Description")
                .available(false)
                .owner(owner)
                .build();

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(updatedItem);

        ItemDto result = itemService.update(1L, updateDto, owner.getId());

        assertNotNull(result);
        assertEquals(updatedItem.getId(), result.getId());
        assertEquals(updatedItem.getName(), result.getName());
        assertEquals(updatedItem.getDescription(), result.getDescription());
        assertEquals(updatedItem.getAvailable(), result.getAvailable());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void update_NonExistingItem_ThrowsNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.update(999L, itemDto, owner.getId()));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void update_NotOwner_ThrowsNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class, () -> itemService.update(1L, itemDto, 999L));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void get_ExistingItem_ReturnsItemDtoWithBookings() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(commentRepository.findByItemId(anyLong())).thenReturn(Collections.emptyList());

        ItemDtoWithBookings result = itemService.get(1L, 2L);

        assertNotNull(result);
        assertEquals(item.getId(), result.getId());
        assertEquals(item.getName(), result.getName());
        assertEquals(item.getDescription(), result.getDescription());
        assertEquals(item.getAvailable(), result.getAvailable());
        assertNull(result.getLastBooking());
        assertNull(result.getNextBooking());
        assertTrue(result.getComments().isEmpty());
    }

    @Test
    void get_NonExistingItem_ThrowsNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.get(999L, 1L));
    }

    @Test
    void getAllByOwner_ReturnsListOfItemDtoWithBookings() {
        when(itemRepository.findByOwner_IdOrderById(anyLong())).thenReturn(List.of(item));
        when(commentRepository.findByItemIdIn(anyList())).thenReturn(Collections.emptyList());

        List<ItemDtoWithBookings> result = itemService.getAllByOwner(owner.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(item.getId(), result.get(0).getId());
        assertEquals(item.getName(), result.get(0).getName());
        assertEquals(item.getDescription(), result.get(0).getDescription());
        assertEquals(item.getAvailable(), result.get(0).getAvailable());
    }

    @Test
    void search_WithText_ReturnsListOfItemDto() {
        when(itemRepository.search(anyString())).thenReturn(List.of(item));

        List<ItemDto> result = itemService.search("Предмет");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(item.getId(), result.get(0).getId());
        assertEquals(item.getName(), result.get(0).getName());
        assertEquals(item.getDescription(), result.get(0).getDescription());
        assertEquals(item.getAvailable(), result.get(0).getAvailable());
    }

    @Test
    void search_WithEmptyText_ReturnsEmptyList() {
        List<ItemDto> result = itemService.search("");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(itemRepository, never()).search(anyString());
    }

    @Test
    void createComment_ValidComment_ReturnsCommentDto() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.existsCompletedBookingByBookerAndItem(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = itemService.createComment(1L, 2L, commentDto);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getText(), result.getText());
        assertEquals(comment.getAuthor().getName(), result.getAuthorName());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_NonExistingUser_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.createComment(1L, 999L, commentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_EmptyText_ThrowsValidationException() {
        CommentDto emptyCommentDto = CommentDto.builder()
                .id(1L)
                .text("")
                .authorName("Автор")
                .created(comment.getCreated())
                .build();

        assertThrows(ValidationException.class, () -> itemService.createComment(1L, 2L, emptyCommentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_NonExistingItem_ThrowsNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.createComment(999L, 2L, commentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_NoCompletedBooking_ThrowsValidationException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.existsCompletedBookingByBookerAndItem(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(false);

        assertThrows(ValidationException.class, () -> itemService.createComment(1L, 2L, commentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }
}