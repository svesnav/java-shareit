package ru.practicum.shareit.booking.dto;

import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponseDto(Long id, LocalDateTime start, LocalDateTime end, BookingStatus status,
                                 ItemSummary item, BookerSummary booker) {
    public record ItemSummary(Long id, String name) {
    }

    public record BookerSummary(Long id) {
    }
}
