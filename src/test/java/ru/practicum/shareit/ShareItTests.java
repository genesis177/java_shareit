package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.*;
import ru.practicum.shareit.user.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShareItTests {

    private UserServiceImpl userService;
    private ItemServiceImpl itemService;

    @BeforeEach
    void setup() {
        UserStorage userStorage = new UserStorage();
        ItemStorage itemStorage = new ItemStorage();
        userService = new UserServiceImpl(userStorage);
        itemService = new ItemServiceImpl(itemStorage, userStorage);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void userCreateAndGet() {
        UserDto user = UserDto.builder().name("Ivan").email("ivan@mail.com").build();
        UserDto created = userService.create(user);
        assertNotNull(created.getId());
        assertEquals("Ivan", created.getName());
        assertEquals("ivan@mail.com", created.getEmail());

        UserDto found = userService.get(created.getId());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void userDuplicateEmailThrowsConflict() {
        userService.create(UserDto.builder().name("A").email("a@mail.ru").build());
        assertThrows(ConflictException.class, () ->
                userService.create(UserDto.builder().name("B").email("a@mail.ru").build()));
    }

    @Test
    void userInvalidEmailThrowsValidation() {
        assertThrows(ValidationException.class, () ->
                userService.create(UserDto.builder().name("A").email("bademail").build()));
    }

    @Test
    void userUpdateEmailToExistingThrowsConflict() {
        UserDto u1 = userService.create(UserDto.builder().name("A").email("a@mail.ru").build());
        UserDto u2 = userService.create(UserDto.builder().name("B").email("b@mail.ru").build());
        assertThrows(ConflictException.class, () ->
                userService.update(u2.getId(), UserDto.builder().email("a@mail.ru").build()));
    }

    @Test
    void itemCreateAndGet() {
        UserDto user = userService.create(UserDto.builder().name("Ivan").email("ivan@mail.com").build());
        ItemDto item = ItemDto.builder().name("Drill").description("Cordless drill").available(true).build();
        ItemDto created = itemService.create(item, user.getId());
        assertNotNull(created.getId());
        assertEquals("Drill", created.getName());
        assertEquals("Cordless drill", created.getDescription());
        assertTrue(created.getAvailable());

        ItemDto found = itemService.get(created.getId(), user.getId());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void itemCreateWithBlankNameThrowsValidation() {
        UserDto user = userService.create(UserDto.builder().name("Ivan").email("ivan@mail.com").build());
        ItemDto item = ItemDto.builder().name(" ").description("desc").available(true).build();
        assertThrows(ValidationException.class, () -> itemService.create(item, user.getId()));
    }

    @Test
    void itemUpdateByNotOwnerThrowsNotFound() {
        UserDto owner = userService.create(UserDto.builder().name("Ivan").email("ivan@mail.com").build());
        UserDto other = userService.create(UserDto.builder().name("Petr").email("petr@mail.com").build());
        ItemDto item = ItemDto.builder().name("Drill").description("Cordless drill").available(true).build();
        ItemDto created = itemService.create(item, owner.getId());
        assertThrows(NotFoundException.class, () ->
                itemService.update(created.getId(), ItemDto.builder().name("NewName").build(), other.getId()));
    }

    @Test
    void itemSearchReturnsCorrectItems() {
        UserDto user = userService.create(UserDto.builder().name("Ivan").email("ivan@mail.com").build());
        itemService.create(ItemDto.builder().name("Drill").description("Cordless drill").available(true).build(), user.getId());
        itemService.create(ItemDto.builder().name("Hammer").description("Big hammer").available(true).build(), user.getId());
        itemService.create(ItemDto.builder().name("Saw").description("Hand saw").available(false).build(), user.getId());

        assertEquals(1, itemService.search("cordless").size());
        assertEquals(1, itemService.search("hammer").size());
        assertEquals(0, itemService.search("saw").size());
    }
}