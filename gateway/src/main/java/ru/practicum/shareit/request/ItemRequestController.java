package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.NewItemRequestDto;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static ru.practicum.shareit.client.BaseClient.USER_HEADER;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final BaseClient client;

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader(USER_HEADER) long userId, @Valid @RequestBody NewItemRequestDto dto) {
        return client.send(POST, "/requests", userId, dto);
    }

    @GetMapping
    public ResponseEntity<byte[]> getOwn(@RequestHeader(USER_HEADER) long userId) {
        return client.send(GET, "/requests", userId, null);
    }

    @GetMapping("/all")
    public ResponseEntity<byte[]> getOthers(@RequestHeader(USER_HEADER) long userId) {
        return client.send(GET, "/requests/all", userId, null);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<byte[]> getById(@RequestHeader(USER_HEADER) long userId, @PathVariable long requestId) {
        return client.send(GET, "/requests/{id}", userId, null, requestId);
    }
}
