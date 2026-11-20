package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemRequestIntegrationTest {
    @Autowired
    private ItemRequestService service;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRequestRepository requests;
    @Autowired
    private ItemRepository items;
    @Autowired
    private UserRepository users;

    private User author;
    private User responder;

    @BeforeEach
    void setUp() {
        author = users.save(new User(null, "Author", "request-author@example.org"));
        responder = users.save(new User(null, "Responder", "request-responder@example.org"));
    }

    @Test
    void createsRequestWithServerTimeAndNoAnswers() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ItemRequestDto saved = service.create(author.getId(), new NewItemRequestDto("Need a step ladder"));

        assertThat(saved.created()).isBetween(before, LocalDateTime.now());
        assertThat(saved.items()).isEmpty();
        ItemRequest stored = requests.findById(saved.id()).orElseThrow();
        assertThat(stored.getDescription()).isEqualTo("Need a step ladder");
        assertThat(stored.getRequestor().getId()).isEqualTo(author.getId());
    }

    @Test
    void listsOwnAndOtherRequestsInDescendingOrder() {
        LocalDateTime now = LocalDateTime.now();
        ItemRequest early = saveRequest(author, "Earlier", now.minusDays(1));
        ItemRequest latest = saveRequest(author, "Latest", now);
        ItemRequest other = saveRequest(responder, "Someone else's", now.minusHours(1));
        ItemDto answer = itemService.create(responder.getId(),
                new ItemDto(null, "Ladder", "Folding ladder", false, latest.getId()));

        assertThat(service.getOwn(author.getId())).extracting(ItemRequestDto::id)
                .containsExactly(latest.getId(), early.getId());
        assertThat(service.getOthers(author.getId())).extracting(ItemRequestDto::id)
                .containsExactly(other.getId());
        assertThat(service.getOthers(responder.getId())).extracting(ItemRequestDto::id)
                .containsExactly(latest.getId(), early.getId());
        assertThat(service.getOthers(responder.getId()).getFirst().items())
                .containsExactly(new ItemRequestDto.ItemSummary(answer.getId(), "Ladder", responder.getId()));
    }

    @Test
    void anotherUserCanReadLinkedAnswersAndUnlinkedItemsAreExcluded() {
        ItemRequestDto request = service.create(author.getId(), new NewItemRequestDto("Need a drill"));
        ItemDto answer = itemService.create(responder.getId(),
                new ItemDto(null, "Drill", "Cordless drill", true, request.id()));
        ItemDto unrelated = itemService.create(responder.getId(),
                new ItemDto(null, "Saw", "Handsaw", true, null));

        assertThat(answer.getRequestId()).isEqualTo(request.id());
        assertThat(items.findById(unrelated.getId()).orElseThrow().getRequestId()).isNull();
        assertThat(service.getById(responder.getId(), request.id()).items())
                .containsExactly(new ItemRequestDto.ItemSummary(answer.getId(), "Drill", responder.getId()));
        assertThat(service.getOwn(author.getId()).getFirst().items())
                .containsExactly(new ItemRequestDto.ItemSummary(answer.getId(), "Drill", responder.getId()));
    }

    @Test
    void unknownRequestOrAuthorReturnsNotFoundAndInvalidAnswerIsNeverSaved() {
        assertThatThrownBy(() -> service.getById(author.getId(), Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.create(Long.MAX_VALUE, new NewItemRequestDto("Need a drill")))
                .isInstanceOf(NotFoundException.class);
        long count = items.count();
        assertThatThrownBy(() -> itemService.create(responder.getId(),
                new ItemDto(null, "Drill", "Cordless drill", true, Long.MAX_VALUE)))
                .isInstanceOf(NotFoundException.class);
        assertThat(items.count()).isEqualTo(count);
    }

    private ItemRequest saveRequest(User requestor, String description, LocalDateTime created) {
        return requests.save(new ItemRequest(null, description, requestor, created));
    }
}
