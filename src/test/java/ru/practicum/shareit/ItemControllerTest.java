package ru.practicum.shareit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    private ItemDto itemDto;
    private ItemDtoWithBookings itemDtoWithBookings;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        itemDto = ItemDto.builder()
                .id(1L)
                .name("Item")
                .description("Description")
                .available(true)
                .build();

        itemDtoWithBookings = ItemDtoWithBookings.builder()
                .id(1L)
                .name("Item")
                .description("Description")
                .available(true)
                .lastBooking(null)
                .nextBooking(null)
                .comments(Collections.emptyList())
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("Comment text")
                .authorName("Author")
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    void create_ValidItem_ReturnsCreatedItem() throws Exception {
        when(itemService.create(any(ItemDto.class), anyLong())).thenReturn(itemDto);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemDto.getId().intValue())))
                .andExpect(jsonPath("$.name", is(itemDto.getName())))
                .andExpect(jsonPath("$.description", is(itemDto.getDescription())))
                .andExpect(jsonPath("$.available", is(itemDto.getAvailable())));

        verify(itemService).create(any(ItemDto.class), anyLong());
    }

    @Test
    void create_InvalidItem_ReturnsBadRequest() throws Exception {
        when(itemService.create(any(ItemDto.class), anyLong())).thenThrow(new ValidationException("Invalid item"));

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());

        verify(itemService).create(any(ItemDto.class), anyLong());
    }

    @Test
    void update_ValidItem_ReturnsUpdatedItem() throws Exception {
        ItemDto updatedItemDto = ItemDto.builder()
                .id(1L)
                .name("Updated Item")
                .description("Updated Description")
                .available(false)
                .build();

        when(itemService.update(anyLong(), any(ItemDto.class), anyLong())).thenReturn(updatedItemDto);

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(updatedItemDto.getId().intValue())))
                .andExpect(jsonPath("$.name", is(updatedItemDto.getName())))
                .andExpect(jsonPath("$.description", is(updatedItemDto.getDescription())))
                .andExpect(jsonPath("$.available", is(updatedItemDto.getAvailable())));

        verify(itemService).update(anyLong(), any(ItemDto.class), anyLong());
    }

    @Test
    void update_NonExistingItem_ReturnsNotFound() throws Exception {
        when(itemService.update(anyLong(), any(ItemDto.class), anyLong())).thenThrow(new NotFoundException("Item not found"));

        mockMvc.perform(patch("/items/999")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isNotFound());

        verify(itemService).update(anyLong(), any(ItemDto.class), anyLong());
    }

    @Test
    void get_ExistingItem_ReturnsItem() throws Exception {
        when(itemService.get(anyLong(), anyLong())).thenReturn(itemDtoWithBookings);

        mockMvc.perform(get("/items/1")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemDtoWithBookings.getId().intValue())))
                .andExpect(jsonPath("$.name", is(itemDtoWithBookings.getName())))
                .andExpect(jsonPath("$.description", is(itemDtoWithBookings.getDescription())))
                .andExpect(jsonPath("$.available", is(itemDtoWithBookings.getAvailable())));

        verify(itemService).get(anyLong(), anyLong());
    }

    @Test
    void get_NonExistingItem_ReturnsNotFound() throws Exception {
        when(itemService.get(anyLong(), anyLong())).thenThrow(new NotFoundException("Item not found"));

        mockMvc.perform(get("/items/999")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());

        verify(itemService).get(anyLong(), anyLong());
    }

    @Test
    void getAllByOwner_ReturnsListOfItems() throws Exception {
        when(itemService.getAllByOwner(anyLong())).thenReturn(List.of(itemDtoWithBookings));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(itemDtoWithBookings.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is(itemDtoWithBookings.getName())))
                .andExpect(jsonPath("$[0].description", is(itemDtoWithBookings.getDescription())))
                .andExpect(jsonPath("$[0].available", is(itemDtoWithBookings.getAvailable())));

        verify(itemService).getAllByOwner(anyLong());
    }

    @Test
    void search_WithText_ReturnsListOfItems() throws Exception {
        when(itemService.search(anyString())).thenReturn(List.of(itemDto));

        mockMvc.perform(get("/items/search")
                        .param("text", "item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(itemDto.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is(itemDto.getName())))
                .andExpect(jsonPath("$[0].description", is(itemDto.getDescription())))
                .andExpect(jsonPath("$[0].available", is(itemDto.getAvailable())));

        verify(itemService).search(anyString());
    }

    @Test
    void createComment_ValidComment_ReturnsCreatedComment() throws Exception {
        when(itemService.createComment(anyLong(), anyLong(), any(CommentDto.class))).thenReturn(commentDto);

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(commentDto.getId().intValue())))
                .andExpect(jsonPath("$.text", is(commentDto.getText())))
                .andExpect(jsonPath("$.authorName", is(commentDto.getAuthorName())));

        verify(itemService).createComment(anyLong(), anyLong(), any(CommentDto.class));
    }

    @Test
    void createComment_InvalidComment_ReturnsBadRequest() throws Exception {
        when(itemService.createComment(anyLong(), anyLong(), any(CommentDto.class)))
                .thenThrow(new ValidationException("Invalid comment"));

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());

        verify(itemService).createComment(anyLong(), anyLong(), any(CommentDto.class));
    }
}