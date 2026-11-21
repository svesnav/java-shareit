package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static ru.practicum.shareit.client.BaseClient.USER_HEADER;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final BaseClient client;

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader(USER_HEADER) long userId,
                                         @Validated(Create.class) @RequestBody ItemDto dto) {
        return client.send(POST, "/items", userId, dto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<byte[]> update(@RequestHeader(USER_HEADER) long userId, @PathVariable long itemId,
                                         @Validated(Update.class) @RequestBody ItemDto dto) {
        return client.send(PATCH, "/items/{id}", userId, dto, itemId);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<byte[]> getById(@RequestHeader(USER_HEADER) long userId, @PathVariable long itemId) {
        return client.send(GET, "/items/{id}", userId, null, itemId);
    }

    @GetMapping
    public ResponseEntity<byte[]> getByOwner(@RequestHeader(USER_HEADER) long userId) {
        return client.send(GET, "/items", userId, null);
    }

    @GetMapping("/search")
    public ResponseEntity<byte[]> search(@RequestHeader(USER_HEADER) long userId, @RequestParam String text) {
        return client.send(GET, "/items/search?text={text}", userId, null, text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<byte[]> addComment(@RequestHeader(USER_HEADER) long userId, @PathVariable long itemId,
                                             @Valid @RequestBody CommentDto dto) {
        return client.send(POST, "/items/{id}/comment", userId, dto, itemId);
    }
}
