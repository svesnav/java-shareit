package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDetailsDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class ServerMvcTest {
    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final long USER_ID = 12L;
    private static final LocalDateTime DATE = LocalDateTime.of(2027, 1, 10, 12, 0);

    @Autowired
    private MockMvc mvc;
    @MockBean
    private UserService users;
    @MockBean
    private ItemService items;
    @MockBean
    private BookingService bookings;
    @MockBean
    private ItemRequestService requests;

    @Test
    void userRoutesBindParametersAndSerializeServiceResults() throws Exception {
        UserDto user = new UserDto(USER_ID, "Sam", "sam@example.org");
        when(users.create(new UserDto(null, user.getName(), user.getEmail()))).thenReturn(user);
        when(users.getById(USER_ID)).thenReturn(user);
        when(users.getAll()).thenReturn(List.of(user));
        when(users.update(USER_ID, new UserDto(null, "Pat", null)))
                .thenReturn(new UserDto(USER_ID, "Pat", user.getEmail()));

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sam\",\"email\":\"sam@example.org\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(USER_ID));
        mvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(USER_ID));
        mvc.perform(patch("/users/{id}", USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Pat"));
        mvc.perform(delete("/users/{id}", USER_ID)).andExpect(status().isOk());
        verify(users).delete(USER_ID);
    }

    @Test
    void itemRoutesBindRequestIdAndSerializeComments() throws Exception {
        long itemId = 7L;
        ItemDto input = new ItemDto(null, "Drill", "Cordless", true, 42L);
        ItemDto saved = new ItemDto(itemId, "Drill", "Cordless", true, 42L);
        CommentDto comment = new CommentDto(4L, "Worked well", "Sam", DATE);
        ItemDetailsDto details = new ItemDetailsDto(itemId, "Drill", "Cordless", true,
                42L, null, null, List.of(comment));
        when(items.create(USER_ID, input)).thenReturn(saved);
        when(items.update(USER_ID, itemId, new ItemDto(null, null, null, false, null)))
                .thenReturn(new ItemDto(itemId, "Drill", "Cordless", false, 42L));
        when(items.getById(USER_ID, itemId)).thenReturn(details);
        when(items.getByOwner(USER_ID)).thenReturn(List.of(details));
        when(items.search("drill")).thenReturn(List.of(saved));
        when(items.addComment(USER_ID, itemId, new CommentDto(null, "Worked well", null, null))).thenReturn(comment);

        mvc.perform(post("/items").header(USER_HEADER, USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Cordless\",\"available\":true,\"requestId\":42}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requestId").value(42));
        mvc.perform(patch("/items/{id}", itemId).header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/items/{id}", itemId).header(USER_HEADER, USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comments[0].text").value("Worked well"));
        mvc.perform(get("/items").header(USER_HEADER, USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(itemId));
        mvc.perform(get("/items/search").header(USER_HEADER, USER_ID).param("text", "drill"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Drill"));
        mvc.perform(post("/items/{id}/comment", itemId).header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Worked well\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorName").value("Sam"));
    }

    @Test
    void bookingRoutesBindDatesStatusAndDecision() throws Exception {
        long bookingId = 2L;
        BookingResponseDto.ItemSummary item = new BookingResponseDto.ItemSummary(7L, "Drill");
        BookingResponseDto.BookerSummary booker = new BookingResponseDto.BookerSummary(USER_ID);
        BookingResponseDto booking = new BookingResponseDto(bookingId, DATE, DATE.plusHours(1), BookingStatus.WAITING,
                item, booker);
        BookingResponseDto approved = new BookingResponseDto(bookingId, DATE, DATE.plusHours(1), BookingStatus.APPROVED,
                item, booker);
        when(bookings.create(USER_ID, new BookingDto(7L, DATE, DATE.plusHours(1)))).thenReturn(booking);
        when(bookings.approve(USER_ID, bookingId, true)).thenReturn(approved);
        when(bookings.getById(USER_ID, bookingId)).thenReturn(booking);
        when(bookings.getByBooker(USER_ID, BookingState.ALL)).thenReturn(List.of(booking));
        when(bookings.getByOwner(USER_ID, BookingState.CURRENT)).thenReturn(List.of(approved));

        mvc.perform(post("/bookings").header(USER_HEADER, USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":7,\"start\":\"2027-01-10T12:00:00\",\"end\":\"2027-01-10T13:00:00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING"));
        mvc.perform(patch("/bookings/{id}", bookingId).header(USER_HEADER, USER_ID).param("approved", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/bookings/{id}", bookingId).header(USER_HEADER, USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.item.id").value(7));
        mvc.perform(get("/bookings").header(USER_HEADER, USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].booker.id").value(USER_ID));
        mvc.perform(get("/bookings/owner").header(USER_HEADER, USER_ID).param("state", "CURRENT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void requestRoutesSerializeOwnOthersAndAnswersWithTheSameShape() throws Exception {
        ItemRequestDto created = new ItemRequestDto(42L, "Need a drill", DATE, List.of());
        ItemRequestDto answered = new ItemRequestDto(42L, "Need a drill", DATE,
                List.of(new ItemRequestDto.ItemSummary(7L, "Drill", 13L)));
        when(requests.create(USER_ID, new NewItemRequestDto("Need a drill"))).thenReturn(created);
        when(requests.getOwn(USER_ID)).thenReturn(List.of(answered));
        when(requests.getOthers(13L)).thenReturn(List.of(answered));
        when(requests.getById(13L, 42L)).thenReturn(answered);
        when(requests.getById(USER_ID, 5L)).thenThrow(new NotFoundException("Item request not found"));

        mvc.perform(post("/requests").header(USER_HEADER, USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Need a drill\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value("2027-01-10T12:00:00"))
                .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/requests").header(USER_HEADER, USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].items[0].ownerId").value(13));
        mvc.perform(get("/requests/all").header(USER_HEADER, 13))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].description").value("Need a drill"));
        mvc.perform(get("/requests/42").header(USER_HEADER, 13))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].name").value("Drill"));
        mvc.perform(get("/requests/5").header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Item request not found"));
    }
}
