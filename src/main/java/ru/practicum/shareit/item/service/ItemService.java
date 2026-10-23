package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDetailsDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto create(long userId, ItemDto dto);

    ItemDto update(long userId, long itemId, ItemDto dto);

    ItemDetailsDto getById(long userId, long itemId);

    List<ItemDetailsDto> getByOwner(long userId);

    List<ItemDto> search(String text);

    CommentDto addComment(long userId, long itemId, CommentDto dto);
}
