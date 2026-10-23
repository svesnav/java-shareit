package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentDto;

public final class CommentMapper {
    private CommentMapper() {
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(comment.getId(), comment.getText(), comment.getAuthor().getName(), comment.getCreated());
    }
}
