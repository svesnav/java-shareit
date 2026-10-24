package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;

public final class CommentMapper {
    private CommentMapper() {
    }

    public static Comment toModel(CommentDto dto, Item item, User author, LocalDateTime created) {
        return new Comment(null, dto.text(), item, author, created);
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(comment.getId(), comment.getText(), comment.getAuthor().getName(), comment.getCreated());
    }
}
