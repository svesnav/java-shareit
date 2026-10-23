package ru.practicum.shareit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShareItTests {
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private ItemRepository items;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    private User owner;
    private User booker;
    private User outsider;
    private Item item;

    @BeforeEach
    void setUp() {
        for (String table : List.of("comments", "bookings", "items", "requests", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        owner = users.save(new User(null, "Owner", "owner@example.com"));
        booker = users.save(new User(null, "Booker", "booker@example.com"));
        outsider = users.save(new User(null, "Other", "other@example.com"));
        item = items.save(new Item(null, "Drill", "Concrete tool", true, owner, null));
    }

    @Test
    void userCrudPersistsDataAndRejectsDuplicateEmailAtomically() throws Exception {
        JsonNode created = read(mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("id", 999, "name", "New", "email", "new@example.com"))))
                .andExpect(status().isOk()).andReturn());
        long id = created.get("id").asLong();
        assertThat(id).isNotEqualTo(999);
        assertThat(users.findById(id).orElseThrow().getEmail()).isEqualTo("new@example.com");
        mvc.perform(patch("/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Updated"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.com"));
        mvc.perform(patch("/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Bad update", "email", owner.getEmail()))))
                .andExpect(status().isConflict());
        mvc.perform(get("/users/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Duplicate", "email", "OWNER@example.com"))))
                .andExpect(status().isConflict());
        mvc.perform(patch("/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "changed@example.com"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(delete("/users/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/users/{id}", id)).andExpect(status().isNotFound());
        assertThat(users.existsById(id)).isFalse();
    }

    @Test
    void itemCrudAndSearchContinueWorkingWithJpa() throws Exception {
        JsonNode created = read(mvc.perform(post("/items").header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Screwdriver", "description", "Drill accessory", "available", true))))
                .andExpect(status().isOk()).andReturn());
        long id = created.get("id").asLong();
        assertThat(items.existsById(id)).isTrue();
        mvc.perform(patch("/items/{id}", id).header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Stolen"))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/items/{id}", id).header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("available", false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Screwdriver"));
        mvc.perform(get("/items/search").header(USER_HEADER, booker.getId()).param("text", "dRiLl"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(item.getId()));
        mvc.perform(patch("/items/{id}", id).header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("available", true))))
                .andExpect(status().isOk());
        mvc.perform(get("/items/search").header(USER_HEADER, booker.getId()).param("text", "DRILL"))
                .andExpect(jsonPath("$", hasSize(2)));
        for (String text : List.of("", "   ", "%", "_", "missing")) {
            mvc.perform(get("/items/search").header(USER_HEADER, booker.getId()).param("text", text))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        }
        mvc.perform(get("/items/{id}", id).header(USER_HEADER, booker.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comments", hasSize(0)));
        mvc.perform(get("/items").header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void blankUpdatesAreRejectedWithoutChangingStoredData(String blank) throws Exception {
        mvc.perform(patch("/users/{id}", owner.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", blank))))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/users/{id}", owner.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", blank))))
                .andExpect(status().isBadRequest());
        for (String field : List.of("name", "description")) {
            mvc.perform(patch("/items/{id}", item.getId()).header(USER_HEADER, owner.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(field, blank))))
                    .andExpect(status().isBadRequest());
        }
        assertThat(users.findById(owner.getId()).orElseThrow().getName()).isEqualTo("Owner");
        assertThat(items.findById(item.getId()).orElseThrow().getDescription()).isEqualTo("Concrete tool");
    }

    @Test
    void bookingIsCreatedWaitingAndVisibleOnlyToParticipants() throws Exception {
        long id = createBooking();
        mvc.perform(get("/bookings/{id}", id).header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.item.id").value(item.getId()))
                .andExpect(jsonPath("$.booker.id").value(booker.getId()));
        mvc.perform(get("/bookings/{id}", id).header(USER_HEADER, booker.getId())).andExpect(status().isOk());
        mvc.perform(get("/bookings/{id}", id).header(USER_HEADER, outsider.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/bookings/{id}", id).header(USER_HEADER, Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(get("/bookings/{id}", Long.MAX_VALUE).header(USER_HEADER, owner.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/bookings").header(USER_HEADER, booker.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/bookings/owner").header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/bookings/owner").header(USER_HEADER, outsider.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void onlyOwnerCanDecideOnce(boolean approved) throws Exception {
        long id = createBooking();
        for (User user : List.of(booker, outsider)) {
            mvc.perform(patch("/bookings/{id}", id).header(USER_HEADER, user.getId())
                            .param("approved", String.valueOf(approved)))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(patch("/bookings/{id}", id).header(USER_HEADER, owner.getId()))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/bookings/{id}", id).header(USER_HEADER, owner.getId()).param("approved", "invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/bookings/{id}", id).header(USER_HEADER, owner.getId())
                        .param("approved", String.valueOf(approved)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(approved ? "APPROVED" : "REJECTED"));
        mvc.perform(patch("/bookings/{id}", id).header(USER_HEADER, owner.getId())
                        .param("approved", String.valueOf(!approved)))
                .andExpect(status().isBadRequest());
        assertThat(bookings.findById(id).orElseThrow().getStatus())
                .isEqualTo(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
    }

    @Test
    void invalidDatesAndMissingBookingFieldsAreRejected() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        for (LocalDateTime[] dates : List.of(
                new LocalDateTime[]{now.minusDays(1), now.plusDays(1)},
                new LocalDateTime[]{now.plusDays(1), now.plusDays(1)},
                new LocalDateTime[]{now.plusDays(2), now.plusDays(1)},
                new LocalDateTime[]{now.minusDays(2), now.minusDays(1)})) {
            mvc.perform(post("/bookings").header(USER_HEADER, booker.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(bookingBody(dates[0], dates[1]))))
                    .andExpect(status().isBadRequest());
        }
        for (String field : List.of("itemId", "start", "end")) {
            ObjectNode body = bookingBody(now.plusDays(1), now.plusDays(2));
            body.remove(field);
            mvc.perform(post("/bookings").header(USER_HEADER, booker.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(bookings.count()).isZero();
    }

    @Test
    void unavailableOwnAndMissingItemsCannotBeBooked() throws Exception {
        ObjectNode body = bookingBody(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        mvc.perform(post("/bookings").header(USER_HEADER, owner.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/bookings").header(USER_HEADER, Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isNotFound());
        item.setAvailable(false);
        items.save(item);
        mvc.perform(post("/bookings").header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
        body.put("itemId", Long.MAX_VALUE);
        mvc.perform(post("/bookings").header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isNotFound());
        assertThat(bookings.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(BookingState.class)
    void bookingFiltersApplyToBookerAndAllOwnerItemsInDescendingOrder(BookingState state) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Booking past = seed(item, booker, BookingStatus.APPROVED, now.minusDays(4), now.minusDays(3));
        Booking current = seed(item, booker, BookingStatus.APPROVED, now.minusHours(1), now.plusHours(1));
        Booking future = seed(item, booker, BookingStatus.APPROVED, now.plusDays(1), now.plusDays(2));
        Item second = items.save(new Item(null, "Second", "Tool", true, owner, null));
        Booking waiting = seed(second, booker, BookingStatus.WAITING, now.plusDays(3), now.plusDays(4));
        Booking rejected = seed(second, booker, BookingStatus.REJECTED, now.plusDays(5), now.plusDays(6));
        Item foreign = items.save(new Item(null, "Foreign", "Tool", true, outsider, null));
        seed(foreign, owner, BookingStatus.WAITING, now.plusDays(7), now.plusDays(8));
        List<Long> expected = switch (state) {
            case ALL -> List.of(rejected.getId(), waiting.getId(), future.getId(), current.getId(), past.getId());
            case CURRENT -> List.of(current.getId());
            case PAST -> List.of(past.getId());
            case FUTURE -> List.of(rejected.getId(), waiting.getId(), future.getId());
            case WAITING -> List.of(waiting.getId());
            case REJECTED -> List.of(rejected.getId());
        };
        for (boolean byOwner : List.of(false, true)) {
            JsonNode result = read(mvc.perform(get(byOwner ? "/bookings/owner" : "/bookings")
                            .header(USER_HEADER, byOwner ? owner.getId() : booker.getId()).param("state", state.name()))
                    .andExpect(status().isOk()).andReturn());
            List<Long> actual = new java.util.ArrayList<>();
            result.forEach(node -> actual.add(node.get("id").asLong()));
            assertThat(actual).containsExactlyElementsOf(expected);
        }
    }

    @Test
    void invalidStateAndMissingHeadersReturnBadRequest() throws Exception {
        for (String path : List.of("/bookings", "/bookings/owner")) {
            mvc.perform(get(path).header(USER_HEADER, owner.getId()).param("state", "UNKNOWN"))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(path)).andExpect(status().isBadRequest());
            mvc.perform(get(path).header(USER_HEADER, Long.MAX_VALUE)).andExpect(status().isNotFound());
        }
    }

    @Test
    void ownerSeesNearestApprovedBookingsButOtherUsersDoNot() throws Exception {
        mvc.perform(get("/items/{id}", item.getId()).header(USER_HEADER, owner.getId()))
                .andExpect(jsonPath("$.lastBooking").value(nullValue()))
                .andExpect(jsonPath("$.nextBooking").value(nullValue()));
        LocalDateTime now = LocalDateTime.now();
        seed(item, booker, BookingStatus.APPROVED, now.minusDays(4), now.minusDays(3));
        Booking current = seed(item, booker, BookingStatus.APPROVED, now.minusHours(2), now.plusHours(2));
        Booking next = seed(item, booker, BookingStatus.APPROVED, now.plusDays(1), now.plusDays(2));
        seed(item, booker, BookingStatus.APPROVED, now.plusDays(5), now.plusDays(6));
        seed(item, booker, BookingStatus.WAITING, now.plusHours(3), now.plusHours(4));
        seed(item, booker, BookingStatus.REJECTED, now.minusHours(1), now.plusHours(1));
        mvc.perform(get("/items/{id}", item.getId()).header(USER_HEADER, owner.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastBooking.id").value(current.getId()))
                .andExpect(jsonPath("$.nextBooking.id").value(next.getId()))
                .andExpect(jsonPath("$.nextBooking.start").exists())
                .andExpect(jsonPath("$.nextBooking.end").exists())
                .andExpect(jsonPath("$.nextBooking.bookerId").value(booker.getId()));
        mvc.perform(get("/items").header(USER_HEADER, owner.getId()))
                .andExpect(jsonPath("$[0].lastBooking.id").value(current.getId()))
                .andExpect(jsonPath("$[0].nextBooking.id").value(next.getId()));
        mvc.perform(get("/items/{id}", item.getId()).header(USER_HEADER, booker.getId()))
                .andExpect(jsonPath("$.lastBooking").value(nullValue()))
                .andExpect(jsonPath("$.nextBooking").value(nullValue()));
    }

    @Test
    void completedBookingAllowsCommentAndBothItemViewsShowIt() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        seed(item, booker, BookingStatus.APPROVED, now.minusDays(2), now.minusDays(1));
        JsonNode response = read(mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "Worked well", "authorName", "Fake", "id", 999))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorName").value("Booker"))
                .andExpect(jsonPath("$.created").exists()).andReturn());
        assertThat(response.get("id").asLong()).isNotEqualTo(999);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments", Long.class)).isEqualTo(1);
        mvc.perform(get("/items/{id}", item.getId()).header(USER_HEADER, outsider.getId()))
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].text").value("Worked well"))
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));
        mvc.perform(get("/items").header(USER_HEADER, owner.getId()))
                .andExpect(jsonPath("$[0].comments", hasSize(1)));
        for (String blank : List.of("", "  ", "\t")) {
            mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, booker.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("text", blank))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"WAITING", "REJECTED", "CANCELED"})
    void unapprovedPastBookingDoesNotAllowComment(BookingStatus bookingStatus) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        seed(item, booker, bookingStatus, now.minusDays(2), now.minusDays(1));
        mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("text", "Review"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void futureCurrentOtherItemAndOtherUserBookingsDoNotAllowComment() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        seed(item, booker, BookingStatus.APPROVED, now.plusDays(1), now.plusDays(2));
        seed(item, booker, BookingStatus.APPROVED, now.minusHours(1), now.plusHours(1));
        seed(item, outsider, BookingStatus.APPROVED, now.minusDays(2), now.minusDays(1));
        Item second = items.save(new Item(null, "Second", "Tool", true, owner, null));
        seed(second, booker, BookingStatus.APPROVED, now.minusDays(2), now.minusDays(1));
        for (User user : List.of(booker, owner)) {
            mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, user.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("text", "Review"))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/items/{id}/comment", Long.MAX_VALUE).header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("text", "Review"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("text", "Review"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void schemaIsRepeatableAndDatabaseEnforcesConstraints() {
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        schema.execute(dataSource);
        schema.execute(dataSource);
        assertThat(users.findById(owner.getId()).orElseThrow().getName()).isEqualTo("Owner");
        assertThat(items.existsById(item.getId())).isTrue();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "Duplicate", owner.getEmail()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO items (name, description, is_available, owner_id) VALUES (?, ?, ?, ?)",
                "Invalid", "Missing owner", true, Long.MAX_VALUE)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO bookings (start_date, end_date, item_id, booker_id, status) VALUES (?, ?, ?, ?, ?)",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1),
                item.getId(), booker.getId(), "WAITING")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingOwnerRemovesDependentData() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        seed(item, booker, BookingStatus.APPROVED, now.minusDays(2), now.minusDays(1));
        mvc.perform(post("/items/{id}/comment", item.getId()).header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("text", "Review"))))
                .andExpect(status().isOk());
        mvc.perform(delete("/users/{id}", owner.getId())).andExpect(status().isOk());
        assertThat(items.count()).isZero();
        assertThat(bookings.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments", Long.class)).isZero();
    }

    private long createBooking() throws Exception {
        ObjectNode body = bookingBody(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        body.put("status", "APPROVED");
        JsonNode response = read(mvc.perform(post("/bookings").header(USER_HEADER, booker.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING")).andReturn());
        return response.get("id").asLong();
    }

    private ObjectNode bookingBody(LocalDateTime start, LocalDateTime end) {
        return mapper.createObjectNode().put("itemId", item.getId()).put("start", start.toString()).put("end", end.toString());
    }

    private Booking seed(Item bookedItem, User borrower, BookingStatus bookingStatus, LocalDateTime start, LocalDateTime end) {
        return bookings.save(new Booking(null, start, end, bookedItem, borrower, bookingStatus));
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
