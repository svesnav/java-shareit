package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

public final class BookingMapper {
    private BookingMapper() {
    }

    public static Booking toModel(BookingDto dto, Item item, User booker) {
        return new Booking(null, dto.getStart(), dto.getEnd(), item, booker, BookingStatus.WAITING);
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
