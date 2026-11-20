package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
import ru.practicum.shareit.client.BaseClient;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static ru.practicum.shareit.client.BaseClient.USER_HEADER;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BaseClient client;

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader(USER_HEADER) long userId, @Valid @RequestBody BookingDto dto) {
        return client.send(POST, "/bookings", userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<byte[]> approve(@RequestHeader(USER_HEADER) long userId, @PathVariable long bookingId,
                                          @RequestParam boolean approved) {
        return client.send(PATCH, "/bookings/{id}?approved={approved}", userId, null, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<byte[]> getById(@RequestHeader(USER_HEADER) long userId, @PathVariable long bookingId) {
        return client.send(GET, "/bookings/{id}", userId, null, bookingId);
    }

    @GetMapping
    public ResponseEntity<byte[]> getByBooker(@RequestHeader(USER_HEADER) long userId,
                                              @RequestParam(defaultValue = "ALL") BookingState state) {
        return client.send(GET, "/bookings?state={state}", userId, null, state);
    }

    @GetMapping("/owner")
    public ResponseEntity<byte[]> getByOwner(@RequestHeader(USER_HEADER) long userId,
                                             @RequestParam(defaultValue = "ALL") BookingState state) {
        return client.send(GET, "/bookings/owner?state={state}", userId, null, state);
    }
}
