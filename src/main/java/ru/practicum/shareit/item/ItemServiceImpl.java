package ru.practicum.shareit.item;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    public ItemServiceImpl(ItemRepository itemRepository, UserRepository userRepository,
                           BookingRepository bookingRepository, CommentRepository commentRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public ItemDto create(ItemDto itemDto, Long ownerId) {
        validate(itemDto, true);
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Item item = ItemMapper.toItem(itemDto, owner);
        item.setId(null);
        return ItemMapper.toDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(Long itemId, ItemDto itemDto, Long ownerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!item.getOwner().getId().equals(ownerId)) {
            throw new NotFoundException("Вы не являетесь владельцем вещи");
        }

        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toDto(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDtoWithBookings get(Long itemId, Long userId) {
        try {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

            // Получаем комментарии для вещи
            List<CommentDto> comments = commentRepository.findByItemId(itemId).stream()
                    .map(CommentMapper::toDto)
                    .collect(Collectors.toList());

            // Если пользователь не владелец, не показываем информацию о бронированиях
            if (!item.getOwner().getId().equals(userId)) {
                return ItemMapper.toDtoWithBookings(item, null, null, comments);
            }

            LocalDateTime now = LocalDateTime.now();

            // Получаем последнее бронирование
            List<Booking> lastBookings = bookingRepository.findLastBookingForItem(itemId, now);
            Booking lastBooking = lastBookings.isEmpty() ? null : lastBookings.get(0);

            // Получаем следующее бронирование
            List<Booking> nextBookings = bookingRepository.findNextBookingForItem(itemId, now);
            Booking nextBooking = nextBookings.isEmpty() ? null : nextBookings.get(0);

            return ItemMapper.toDtoWithBookings(
                    item,
                    lastBooking != null ? BookingMapper.toShortDto(lastBooking) : null,
                    nextBooking != null ? BookingMapper.toShortDto(nextBooking) : null,
                    comments
            );
        } catch (Exception e) {
            // Добавляем логирование для отладки
            System.err.println("Ошибка при получении вещи: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDtoWithBookings> getAllByOwner(Long ownerId) {
        List<Item> items = itemRepository.findByOwner_IdOrderById(ownerId);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        // Получаем все комментарии для всех вещей
        Map<Long, List<CommentDto>> commentsByItemId = commentRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())
                ));

        return items.stream()
                .map(item -> {
                    // Получаем последнее бронирование
                    List<Booking> lastBookings = bookingRepository.findLastBookingForItem(item.getId(), now);
                    Booking lastBooking = lastBookings.isEmpty() ? null : lastBookings.get(0);

                    // Получаем следующее бронирование
                    List<Booking> nextBookings = bookingRepository.findNextBookingForItem(item.getId(), now);
                    Booking nextBooking = nextBookings.isEmpty() ? null : nextBookings.get(0);

                    return ItemMapper.toDtoWithBookings(
                            item,
                            lastBooking != null ? BookingMapper.toShortDto(lastBooking) : null,
                            nextBooking != null ? BookingMapper.toShortDto(nextBooking) : null,
                            commentsByItemId.getOrDefault(item.getId(), Collections.emptyList())
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long itemId, Long userId, CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Comment text is required");
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        LocalDateTime now = LocalDateTime.now();

        // Проверяем, что пользователь брал вещь в аренду и аренда уже завершена
        boolean hasCompletedBooking = bookingRepository.existsCompletedBookingByBookerAndItem(userId, itemId, now);

        if (!hasCompletedBooking) {
            throw new ValidationException("Вы не можете оставить комментарий к вещи, которую не брали в аренду");
        }

        List<Booking> approvedBookings = bookingRepository.findByBookerIdAndStatus(
                userId, BookingStatus.APPROVED, Sort.by(Sort.Direction.DESC, "start"));

        boolean hasApprovedBooking = approvedBookings.stream()
                .anyMatch(b -> b.getItem().getId().equals(itemId));

        if (!hasApprovedBooking) {

        }

        Comment comment = Comment.builder()
                .text(commentDto.getText())
                .item(item)
                .author(author)
                .created(now)
                .build();

        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public List<ItemDto> getAll(Long userId) {
        return List.of();
    }

    private void validate(ItemDto itemDto, boolean isCreate) {
        if (isCreate) {
            if (itemDto.getName() == null || itemDto.getName().isBlank()) {
                throw new ValidationException("Название вещи не может быть пустым");
            }
            if (itemDto.getDescription() == null || itemDto.getDescription().isBlank()) {
                throw new ValidationException("Описание вещи не может быть пустым");
            }
            if (itemDto.getAvailable() == null) {
                throw new ValidationException("Статус доступности вещи должен быть указан");
            }
        }
    }
}