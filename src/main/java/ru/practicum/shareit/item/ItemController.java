package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDetailsDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ItemDto create(@RequestHeader("X-Sharer-User-Id") long userId,
                          @Validated(Create.class) @RequestBody ItemDto dto) {
        log.info("POST /items: userId={}", userId);
        return itemService.create(userId, dto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@RequestHeader("X-Sharer-User-Id") long userId, @PathVariable long itemId,
                          @Validated(Update.class) @RequestBody ItemDto dto) {
        log.info("PATCH /items/{}: userId={}", itemId, userId);
        return itemService.update(userId, itemId, dto);
    }

    @GetMapping("/{itemId}")
    public ItemDetailsDto getById(@RequestHeader("X-Sharer-User-Id") long userId, @PathVariable long itemId) {
        log.info("GET /items/{}: userId={}", itemId, userId);
        return itemService.getById(userId, itemId);
    }

    @GetMapping
    public List<ItemDetailsDto> getByOwner(@RequestHeader("X-Sharer-User-Id") long userId) {
        log.info("GET /items: userId={}", userId);
        return itemService.getByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestHeader("X-Sharer-User-Id") long userId, @RequestParam String text) {
        log.info("GET /items/search: userId={}", userId);
        return itemService.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@RequestHeader("X-Sharer-User-Id") long userId, @PathVariable long itemId,
                                 @Valid @RequestBody CommentDto dto) {
        log.info("POST /items/{}/comment: userId={}", itemId, userId);
        return itemService.addComment(userId, itemId, dto);
    }
}
