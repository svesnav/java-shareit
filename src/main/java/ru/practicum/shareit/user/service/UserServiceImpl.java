package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        checkName(dto.getName());
        checkEmail(dto.getEmail(), null);
        User user = UserMapper.toModel(dto);
        user.setId(null);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto update(long userId, UserDto dto) {
        User existing = findUser(userId);
        String email = dto.getEmail() == null ? existing.getEmail() : dto.getEmail();
        checkEmail(email, userId);
        String name = dto.getName() == null ? existing.getName() : dto.getName();
        checkName(name);
        existing.setName(name);
        existing.setEmail(email);
        return UserMapper.toDto(userRepository.save(existing));
    }

    @Override
    public UserDto getById(long userId) {
        return UserMapper.toDto(findUser(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserMapper::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(long userId) {
        userRepository.delete(findUser(userId));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void checkName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("User name must not be blank or longer than 255 characters");
        }
    }

    private void checkEmail(String email, Long userId) {
        if (email == null || email.isBlank() || !email.contains("@") || email.length() > 512) {
            throw new IllegalArgumentException("Email must not be blank, must contain @ and fit in 512 characters");
        }
        boolean exists = userId == null ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId);
        if (exists) {
            throw new ConflictException("Email is already in use: " + email);
        }
    }
}
