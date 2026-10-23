package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;

public final class BookingMapper {
    private BookingMapper() {
    }

    public static BookingResponseDto toDto(Booking booking) {
        return new BookingResponseDto(booking.getId(), booking.getStart(), booking.getEnd(), booking.getStatus(),
                new BookingResponseDto.ItemSummary(booking.getItem().getId(), booking.getItem().getName()),
                new BookingResponseDto.BookerSummary(booking.getBooker().getId()));
    }

    public static BookingShortDto toShortDto(Booking booking) {
        return new BookingShortDto(booking.getId(), booking.getBooker().getId(), booking.getStart(), booking.getEnd());
    }
}
