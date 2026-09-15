package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.InMemoryItemRepository;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.InMemoryUserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final InMemoryItemRepository itemRepository;
    private final InMemoryUserRepository userRepository;

    @Override
    public ItemDto create(long userId, ItemDto dto) {
        Item item = ItemMapper.toModel(dto, findUser(userId));
        item.setId(null);
        return ItemMapper.toDto(itemRepository.save(item));
    }

    @Override
    public synchronized ItemDto update(long userId, long itemId, ItemDto dto) {
        findUser(userId);
        Item existing = findItem(itemId);
        if (existing.getOwner().getId() != userId) {
            throw new ForbiddenException("Only the owner can update item: " + itemId);
        }
        if (dto.getName() != null && dto.getName().isBlank()) {
            throw new IllegalArgumentException("Item name must not be blank");
        }
        if (dto.getDescription() != null && dto.getDescription().isBlank()) {
            throw new IllegalArgumentException("Item description must not be blank");
        }
        Item updated = new Item(itemId,
                dto.getName() == null ? existing.getName() : dto.getName(),
                dto.getDescription() == null ? existing.getDescription() : dto.getDescription(),
                dto.getAvailable() == null ? existing.getAvailable() : dto.getAvailable(),
                existing.getOwner(), existing.getRequest());
        return ItemMapper.toDto(itemRepository.save(updated));
    }

    @Override
    public ItemDto getById(long itemId) {
        return ItemMapper.toDto(findItem(itemId));
    }

    @Override
    public List<ItemDto> getByOwner(long userId) {
        findUser(userId);
        return itemRepository.findByOwnerId(userId).stream().map(ItemMapper::toDto).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.searchAvailable(text).stream().map(ItemMapper::toDto).toList();
    }

    private Item findItem(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
