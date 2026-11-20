package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceTest {
    private static final long USER_ID = 1L;

    @Mock
    private ItemRequestRepository requests;
    @Mock
    private UserRepository users;
    @Mock
    private ItemRepository items;
    @InjectMocks
    private ItemRequestService service;

    @Test
    void skipsLookingUpAnswersWhenTheUserHasNoRequests() {
        when(users.existsById(USER_ID)).thenReturn(true);
        when(requests.findByRequestorIdOrderByCreatedDescIdDesc(USER_ID)).thenReturn(List.of());

        assertThat(service.getOwn(USER_ID)).isEmpty();
        verifyNoInteractions(items);
    }

    @Test
    void rejectsUnknownUsersBeforeQueryingRequests() {
        when(users.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getOthers(USER_ID))
                .isInstanceOf(NotFoundException.class).hasMessage("User not found: 1");
        verifyNoInteractions(requests, items);
    }

    @Test
    void assignsAnswersToTheMatchingRequestsInOneLookup() {
        User author = new User(USER_ID, "Sam", "sam@example.org");
        User owner = new User(2L, "Pat", "pat@example.org");
        LocalDateTime now = LocalDateTime.now();
        ItemRequest latest = new ItemRequest(21L, "Need a drill", author, now);
        ItemRequest earlier = new ItemRequest(20L, "Need a saw", author, now.minusDays(1));
        Item drill = new Item(31L, "Drill", "Cordless", true, owner, latest.getId());
        Item saw = new Item(32L, "Saw", "Handsaw", true, owner, earlier.getId());
        Item spareDrill = new Item(33L, "Spare drill", "With batteries", true, owner, latest.getId());
        when(users.existsById(USER_ID)).thenReturn(true);
        when(requests.findByRequestorIdOrderByCreatedDescIdDesc(USER_ID)).thenReturn(List.of(latest, earlier));
        when(items.findByRequestIdInOrderByIdAsc(List.of(21L, 20L))).thenReturn(List.of(drill, saw, spareDrill));

        assertThat(service.getOwn(USER_ID)).containsExactly(
                new ItemRequestDto(21L, "Need a drill", now, List.of(
                        new ItemRequestDto.ItemSummary(31L, "Drill", 2L),
                        new ItemRequestDto.ItemSummary(33L, "Spare drill", 2L))),
                new ItemRequestDto(20L, "Need a saw", now.minusDays(1),
                        List.of(new ItemRequestDto.ItemSummary(32L, "Saw", 2L))));
    }
}
