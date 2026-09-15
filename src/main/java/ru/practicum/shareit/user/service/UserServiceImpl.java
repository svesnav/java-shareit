package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.InMemoryUserRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final InMemoryUserRepository userRepository;

    @Override
    public synchronized UserDto create(UserDto dto) {
        checkName(dto.getName());
        checkEmail(dto.getEmail(), null);
        User user = UserMapper.toModel(dto);
        user.setId(null);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    public synchronized UserDto update(long userId, UserDto dto) {
        User existing = findUser(userId);
        String email = dto.getEmail() == null ? existing.getEmail() : dto.getEmail();
        checkEmail(email, userId);
        String name = dto.getName() == null ? existing.getName() : dto.getName();
        checkName(name);
        return UserMapper.toDto(userRepository.save(new User(userId, name, email)));
    }

    @Override
    public UserDto getById(long userId) {
        return UserMapper.toDto(findUser(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toDto).toList();
    }

    @Override
    public synchronized void delete(long userId) {
        findUser(userId);
        userRepository.deleteById(userId);
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void checkName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name must not be blank");
        }
    }

    private void checkEmail(String email, Long userId) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Email must not be blank and must contain @");
        }
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ConflictException("Email is already in use: " + email);
        }
    }
}
