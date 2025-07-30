package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info("Creating item for user {}: {}", userId, itemDto);

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Item item = ItemMapper.toItem(itemDto, owner);

        if (itemDto.getRequestId() != null) {
            ItemRequest request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new NotFoundException("Запрос с id " + itemDto.getRequestId() + " не найден"));
            item.setRequest(request);
        }

        Item savedItem = itemRepository.save(item);
        log.info("Item created with id: {}", savedItem.getId());
        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        log.info("Updating item {} for user {}", itemId, userId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Только владелец может редактировать вещь");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        Item updatedItem = itemRepository.save(item);
        log.info("Item updated: {}", updatedItem.getId());
        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    public ItemDto get(Long userId, Long itemId) {
        log.info("Getting item {} for user {}", itemId, userId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        ItemDto itemDto = ItemMapper.toItemDto(item);

        // Добавляем информацию о бронированиях только для владельца
        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            Optional<Booking> lastBooking = bookingRepository.findLastBookingForItem(itemId, now);
            Optional<Booking> nextBooking = bookingRepository.findNextBookingForItem(itemId, now);

            lastBooking.ifPresent(booking -> itemDto.setLastBooking(BookingMapper.toBookingShortDto(booking)));
            nextBooking.ifPresent(booking -> itemDto.setNextBooking(BookingMapper.toBookingShortDto(booking)));
        }

        // Добавляем комментарии
        List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(itemId);
        itemDto.setComments(comments.stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList()));

        log.info("Retrieved item {} with {} comments", itemId, comments.size());
        return itemDto;
    }

    @Override
    public List<ItemDto> getAll(Long userId) {
        log.info("Getting all items for user {}", userId);

        List<Item> items = itemRepository.findByOwnerIdOrderByIdAsc(userId);
        LocalDateTime now = LocalDateTime.now();

        List<ItemDto> result = items.stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);

                    // Добавляем информацию о бронированиях
                    Optional<Booking> lastBooking = bookingRepository.findLastBookingForItem(item.getId(), now);
                    Optional<Booking> nextBooking = bookingRepository.findNextBookingForItem(item.getId(), now);

                    lastBooking.ifPresent(booking -> itemDto.setLastBooking(BookingMapper.toBookingShortDto(booking)));
                    nextBooking.ifPresent(booking -> itemDto.setNextBooking(BookingMapper.toBookingShortDto(booking)));

                    // Добавляем комментарии
                    List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(item.getId());
                    itemDto.setComments(comments.stream()
                            .map(CommentMapper::toCommentDto)
                            .collect(Collectors.toList()));

                    return itemDto;
                })
                .collect(Collectors.toList());

        log.info("Retrieved {} items for user {}", result.size(), userId);
        return result;
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Searching items with text: {}", text);

        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Item> items = itemRepository.searchByText(text.trim());
        List<ItemDto> result = items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());

        log.info("Found {} items for search text: {}", result.size(), text);
        return result;
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long itemId, CommentDto commentDto) {
        log.info("Creating comment for item {} by user {}", itemId, userId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        // Проверяем, что пользователь бронировал эту вещь и бронирование завершено
        LocalDateTime now = LocalDateTime.now();
        boolean hasBooking = bookingRepository.existsApprovedBookingByUserAndItem(userId, itemId, now);
        if (!hasBooking) {
            throw new BadRequestException("Комментарий может оставить только тот, кто брал вещь в аренду");
        }

        Comment comment = CommentMapper.toComment(commentDto, item, author);
        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created: {}", savedComment.getId());
        return CommentMapper.toCommentDto(savedComment);
    }
}