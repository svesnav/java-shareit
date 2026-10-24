package ru.practicum.shareit.item.dto;

import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.util.List;

public record ItemDetailsDto(Long id, String name, String description, Boolean available,
                             BookingShortDto lastBooking, BookingShortDto nextBooking, List<CommentDto> comments) {
}
