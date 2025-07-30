package ru.practicum.shareit.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String error;
    private String description;
    private String name;

    public ErrorResponse(String error, String description) {
        this.error = error;
        this.description = description;
        this.name = error;
    }

    public ErrorResponse(String error) {
        this.error = error;
        this.name = error;
    }
}