package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final BaseClient client;

    @PostMapping
    public ResponseEntity<byte[]> create(@Validated(Create.class) @RequestBody UserDto dto) {
        return client.send(POST, "/users", null, dto);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<byte[]> update(@PathVariable long userId, @Validated(Update.class) @RequestBody UserDto dto) {
        return client.send(PATCH, "/users/{id}", null, dto, userId);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<byte[]> getById(@PathVariable long userId) {
        return client.send(GET, "/users/{id}", null, null, userId);
    }

    @GetMapping
    public ResponseEntity<byte[]> getAll() {
        return client.send(GET, "/users", null, null);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<byte[]> delete(@PathVariable long userId) {
        return client.send(DELETE, "/users/{id}", null, null, userId);
    }
}
