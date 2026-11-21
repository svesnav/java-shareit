package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto.ItemSummary;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public ItemRequestDto create(long userId, NewItemRequestDto dto) {
        User requestor = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        ItemRequest request = requestRepository.save(
                new ItemRequest(null, dto.description(), requestor, LocalDateTime.now()));
        return toDto(request, List.of());
    }

    public List<ItemRequestDto> getOwn(long userId) {
        checkUserExists(userId);
        return withAnswers(requestRepository.findByRequestorIdOrderByCreatedDescIdDesc(userId));
    }

    public List<ItemRequestDto> getOthers(long userId) {
        checkUserExists(userId);
        return withAnswers(requestRepository.findByRequestorIdNotOrderByCreatedDescIdDesc(userId));
    }

    public ItemRequestDto getById(long userId, long requestId) {
        checkUserExists(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found: " + requestId));
        return withAnswers(List.of(request)).getFirst();
    }

    private List<ItemRequestDto> withAnswers(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        List<Long> ids = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<ItemSummary>> answers = itemRepository.findByRequestIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(Item::getRequestId,
                        Collectors.mapping(item -> new ItemSummary(item.getId(), item.getName(),
                                item.getOwner().getId()), Collectors.toList())));
        return requests.stream().map(request -> toDto(request, answers.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private ItemRequestDto toDto(ItemRequest request, List<ItemSummary> answers) {
        return new ItemRequestDto(request.getId(), request.getDescription(), request.getCreated(), answers);
    }

    private void checkUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
    }
}
