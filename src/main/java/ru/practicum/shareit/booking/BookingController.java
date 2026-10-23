package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public BookingResponseDto create(@RequestHeader("X-Sharer-User-Id") long userId,
                                     @Valid @RequestBody BookingDto dto) {
        log.info("POST /bookings: userId={}, itemId={}", userId, dto.getItemId());
        return bookingService.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public BookingResponseDto approve(@RequestHeader("X-Sharer-User-Id") long userId, @PathVariable long bookingId,
                                      @RequestParam boolean approved) {
        log.info("PATCH /bookings/{}: userId={}, approved={}", bookingId, userId, approved);
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingResponseDto getById(@RequestHeader("X-Sharer-User-Id") long userId, @PathVariable long bookingId) {
        log.info("GET /bookings/{}: userId={}", bookingId, userId);
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingResponseDto> getByBooker(@RequestHeader("X-Sharer-User-Id") long userId,
                                              @RequestParam(defaultValue = "ALL") BookingState state) {
        log.info("GET /bookings: userId={}, state={}", userId, state);
        return bookingService.getByBooker(userId, state);
    }

    @GetMapping("/owner")
    public List<BookingResponseDto> getByOwner(@RequestHeader("X-Sharer-User-Id") long userId,
                                             @RequestParam(defaultValue = "ALL") BookingState state) {
        log.info("GET /bookings/owner: userId={}, state={}", userId, state);
        return bookingService.getByOwner(userId, state);
    }
}
