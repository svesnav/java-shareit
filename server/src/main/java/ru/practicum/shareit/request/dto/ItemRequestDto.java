package ru.practicum.shareit.request.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ItemRequestDto(Long id, String description, LocalDateTime created, List<ItemSummary> items) {
    public record ItemSummary(Long id, String name, Long ownerId) {
    }
}
