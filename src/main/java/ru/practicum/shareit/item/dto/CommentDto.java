package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CommentDto(Long id, @NotBlank @Size(max = 2000) String text, String authorName, LocalDateTime created) {
}
