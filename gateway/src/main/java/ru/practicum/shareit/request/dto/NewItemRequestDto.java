package ru.practicum.shareit.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NewItemRequestDto(
        @NotBlank @Pattern(regexp = "(?s).*\\P{javaWhitespace}.*") @Size(max = 2000) String description) {
}
