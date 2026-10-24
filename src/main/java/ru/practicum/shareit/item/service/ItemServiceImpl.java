package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.CommentMapper;
import ru.practicum.shareit.item.CommentRepository;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDetailsDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(long userId, ItemDto dto) {
        checkText(dto.getName(), "Item name", 255);
        checkText(dto.getDescription(), "Item description", 2000);
        if (dto.getAvailable() == null) {
            throw new IllegalArgumentException("Item availability is required");
        }
        Item item = ItemMapper.toModel(dto, findUser(userId));
        item.setId(null);
        return ItemMapper.toDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(long userId, long itemId, ItemDto dto) {
        findUser(userId);
        Item existing = findItem(itemId);
        if (existing.getOwner().getId() != userId) {
            throw new ForbiddenException("Only the owner can update item: " + itemId);
        }
        String name = dto.getName() == null ? existing.getName() : dto.getName();
        String description = dto.getDescription() == null ? existing.getDescription() : dto.getDescription();
        checkText(name, "Item name", 255);
        checkText(description, "Item description", 2000);
        existing.setName(name);
        existing.setDescription(description);
        if (dto.getAvailable() != null) {
            existing.setAvailable(dto.getAvailable());
        }
        return ItemMapper.toDto(itemRepository.save(existing));
    }

    @Override
    public ItemDetailsDto getById(long userId, long itemId) {
        findUser(userId);
        Item item = findItem(itemId);
        List<Booking> bookings = item.getOwner().getId() == userId
                ? bookingRepository.findByItemIdInAndStatusOrderByStartDescIdDesc(List.of(itemId), BookingStatus.APPROVED)
                : List.of();
        List<Comment> comments = commentRepository.findByItemIdInOrderByCreatedDescIdDesc(List.of(itemId));
        return toDetails(item, bookings, comments, LocalDateTime.now());
    }

    @Override
    public List<ItemDetailsDto> getByOwner(long userId) {
        findUser(userId);
        List<Item> items = itemRepository.findByOwnerIdOrderByIdAsc(userId);
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> ids = items.stream().map(Item::getId).toList();
        Map<Long, List<Booking>> bookings = bookingRepository
                .findByItemIdInAndStatusOrderByStartDescIdDesc(ids, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));
        Map<Long, List<Comment>> comments = commentRepository.findByItemIdInOrderByCreatedDescIdDesc(ids).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId()));
        LocalDateTime now = LocalDateTime.now();
        return items.stream().map(item -> toDetails(item, bookings.getOrDefault(item.getId(), List.of()),
                comments.getOrDefault(item.getId(), List.of()), now)).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.searchAvailable(text).stream().map(ItemMapper::toDto).toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(long userId, long itemId, CommentDto dto) {
        User author = findUser(userId);
        Item item = findItem(itemId);
        LocalDateTime now = LocalDateTime.now();
        if (!bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndLessThanEqual(
                itemId, userId, BookingStatus.APPROVED, now)) {
            throw new IllegalArgumentException("A comment requires a completed approved booking of this item");
        }
        return CommentMapper.toDto(commentRepository.save(CommentMapper.toModel(dto, item, author, now)));
    }

    private ItemDetailsDto toDetails(Item item, List<Booking> bookings, List<Comment> comments, LocalDateTime now) {
        BookingShortDto last = null;
        BookingShortDto next = null;
        for (Booking booking : bookings) {
            if (booking.getStart().isAfter(now)) {
                next = BookingMapper.toShortDto(booking);
            } else if (last == null) {
                last = BookingMapper.toShortDto(booking);
            }
        }
        return new ItemDetailsDto(item.getId(), item.getName(), item.getDescription(), item.getAvailable(),
                last, next, comments.stream().map(CommentMapper::toDto).toList());
    }

    private Item findItem(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void checkText(String text, String field, int maxLength) {
        if (text == null || text.isBlank() || text.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not be blank or longer than " + maxLength + " characters");
        }
    }
}
