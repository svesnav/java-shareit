package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto create(long userId, BookingDto dto);

    BookingResponseDto approve(long userId, long bookingId, boolean approved);

    BookingResponseDto getById(long userId, long bookingId);

    List<BookingResponseDto> getByBooker(long userId, BookingState state);

    List<BookingResponseDto> getByOwner(long userId, BookingState state);
}
