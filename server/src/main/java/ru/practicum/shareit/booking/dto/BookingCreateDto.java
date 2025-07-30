package ru.practicum.shareit.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Future;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateDto {
    @NotNull(message = "ID предмета не может быть null")
    private Long itemId;

    @NotNull(message = "Время начала бронирования не может быть null")
    @FutureOrPresent(message = "Время начала бронирования не может быть в прошлом")
    private LocalDateTime start;

    @NotNull(message = "Время окончания бронирования не может быть null")
    @Future(message = "Время окончания бронирования должно быть в будущем")
    private LocalDateTime end;
}