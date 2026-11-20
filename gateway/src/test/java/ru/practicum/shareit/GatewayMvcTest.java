package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.dto.ItemDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.shareit.client.BaseClient.USER_HEADER;

@WebMvcTest
class GatewayMvcTest {
    private static final long USER_ID = 12L;
    private static final String USER_JSON = "{\"name\":\"Sam\",\"email\":\"sam@example.org\"}";
    private static final String ITEM_JSON = "{\"name\":\"Drill\",\"description\":\"Cordless\","
            + "\"available\":true,\"requestId\":42}";
    private static final String REQUEST_JSON = "{\"description\":\"Need a drill\"}";
    private static final String COMMENT_JSON = "{\"text\":\"Worked well\"}";

    @Autowired
    private MockMvc mvc;
    @MockBean
    private BaseClient client;

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    void forwardsEveryPublicRoute(HttpMethod method, String path, String upstream, Long userId, String body)
            throws Exception {
        when(client.send(eq(method), eq(upstream), eq(userId), any(), any(Object[].class)))
                .thenReturn(jsonResponse(200, "{\"proxied\":true}"));
        MockHttpServletRequestBuilder request = request(method, path);
        if (userId != null) {
            request.header(USER_HEADER, userId);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }

        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.proxied").value(true));
    }

    private static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of(POST, "/users", "/users", null, USER_JSON),
                Arguments.of(PATCH, "/users/3", "/users/{id}", null, "{\"name\":\"Pat\"}"),
                Arguments.of(GET, "/users/3", "/users/{id}", null, null),
                Arguments.of(GET, "/users", "/users", null, null),
                Arguments.of(DELETE, "/users/3", "/users/{id}", null, null),
                Arguments.of(POST, "/items", "/items", USER_ID, ITEM_JSON),
                Arguments.of(PATCH, "/items/7", "/items/{id}", USER_ID, "{\"available\":false}"),
                Arguments.of(GET, "/items/7", "/items/{id}", USER_ID, null),
                Arguments.of(GET, "/items", "/items", USER_ID, null),
                Arguments.of(GET, "/items/search?text=drill", "/items/search?text={text}", USER_ID, null),
                Arguments.of(POST, "/items/7/comment", "/items/{id}/comment", USER_ID, COMMENT_JSON),
                Arguments.of(POST, "/bookings", "/bookings", USER_ID, bookingJson(1, 2)),
                Arguments.of(PATCH, "/bookings/2?approved=false", "/bookings/{id}?approved={approved}", USER_ID, null),
                Arguments.of(GET, "/bookings/2", "/bookings/{id}", USER_ID, null),
                Arguments.of(GET, "/bookings", "/bookings?state={state}", USER_ID, null),
                Arguments.of(GET, "/bookings/owner?state=PAST", "/bookings/owner?state={state}", USER_ID, null),
                Arguments.of(POST, "/requests", "/requests", USER_ID, REQUEST_JSON),
                Arguments.of(GET, "/requests", "/requests", USER_ID, null),
                Arguments.of(GET, "/requests/all", "/requests/all", USER_ID, null),
                Arguments.of(GET, "/requests/42", "/requests/{id}", USER_ID, null));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("invalidBodies")
    void rejectsInvalidBodiesBeforeCallingServer(HttpMethod method, String path, String body) throws Exception {
        mvc.perform(request(method, path).header(USER_HEADER, USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(client);
    }

    private static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of(POST, "/requests", "{}"),
                Arguments.of(POST, "/requests", "{\"description\":\"   \"}"),
                Arguments.of(POST, "/requests", "{\"description\":\"\\u2003\"}"),
                Arguments.of(POST, "/requests", "{\"description\":\"" + "x".repeat(2001) + "\"}"),
                Arguments.of(POST, "/users", "{\"name\":\"Sam\",\"email\":\"invalid\"}"),
                Arguments.of(POST, "/users", "{\"email\":\"sam@example.org\"}"),
                Arguments.of(PATCH, "/users/3", "{\"name\":\" \"}"),
                Arguments.of(PATCH, "/users/3", "{\"name\":\"\\u2003\"}"),
                Arguments.of(PATCH, "/users/3", "{\"email\":\" \"}"),
                Arguments.of(POST, "/items", "{\"name\":\"Drill\",\"description\":\"Cordless\"}"),
                Arguments.of(POST, "/items", ITEM_JSON.replace("42", "0")),
                Arguments.of(POST, "/items", ITEM_JSON.replace("Drill", "\\u2003")),
                Arguments.of(PATCH, "/items/7", "{\"name\":\" \"}"),
                Arguments.of(PATCH, "/items/7", "{\"description\":\" \"}"),
                Arguments.of(POST, "/items/7/comment", "{}"),
                Arguments.of(POST, "/items/7/comment", "{\"text\":\" \"}"),
                Arguments.of(POST, "/bookings", "{}"),
                Arguments.of(POST, "/bookings", bookingJson(-1, 1)),
                Arguments.of(POST, "/bookings", bookingJson(1, 1)),
                Arguments.of(POST, "/bookings", bookingJson(2, 1)),
                Arguments.of(POST, "/bookings", bookingJson(-2, -1)));
    }

    @Test
    void acceptsItemsWithOrWithoutRequestId() throws Exception {
        ItemDto linked = new ItemDto(null, "Drill", "Cordless", true, 42L);
        ItemDto standalone = new ItemDto(null, "Saw", "Hand tool", false, null);
        when(client.send(eq(POST), eq("/items"), eq(USER_ID), any(), any(Object[].class)))
                .thenReturn(jsonResponse(200, "{}"));

        mvc.perform(post("/items").header(USER_HEADER, USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM_JSON))
                .andExpect(status().isOk());
        mvc.perform(post("/items").header(USER_HEADER, USER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Saw\",\"description\":\"Hand tool\",\"available\":false}"))
                .andExpect(status().isOk());
        verify(client).send(POST, "/items", USER_ID, linked);
        verify(client).send(POST, "/items", USER_ID, standalone);
    }

    @Test
    void rejectsUnknownBookingStateAndMissingUserHeader() throws Exception {
        mvc.perform(get("/bookings").header(USER_HEADER, USER_ID).param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/requests/all")).andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test
    void preservesServerErrorStatusAndJson() throws Exception {
        when(client.send(GET, "/requests/{id}", USER_ID, null, 42L))
                .thenReturn(jsonResponse(404, "{\"error\":\"Item request not found\"}"));

        mvc.perform(get("/requests/42").header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Item request not found"));
    }

    private static ResponseEntity<byte[]> jsonResponse(int status, String json) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String bookingJson(int startOffset, int endOffset) {
        LocalDateTime now = LocalDateTime.now();
        return "{\"itemId\":7,\"start\":\"%s\",\"end\":\"%s\"}"
                .formatted(now.plusDays(startOffset), now.plusDays(endOffset));
    }
}
