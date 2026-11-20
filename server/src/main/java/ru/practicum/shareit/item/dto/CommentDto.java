package ru.practicum.shareit.item.dto;

import java.time.LocalDateTime;

public record CommentDto(Long id, String text, String authorName, LocalDateTime created) {
}
