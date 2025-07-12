package ru.practicum.shareit.booking;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookerDto {
    private Long id;
    private String name;
}