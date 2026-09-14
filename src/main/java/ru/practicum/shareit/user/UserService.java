package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final InMemoryUserRepository userRepository;

    public synchronized UserDto create(UserDto dto) {
        checkEmail(dto.getEmail(), null);
        User user = UserMapper.toModel(dto);
        user.setId(null);
        return UserMapper.toDto(userRepository.save(user));
    }

    public synchronized UserDto update(long userId, UserDto dto) {
        User existing = findUser(userId);
        String email = dto.getEmail() == null ? existing.getEmail() : dto.getEmail();
        checkEmail(email, userId);
        String name = dto.getName() == null ? existing.getName() : dto.getName();
        return UserMapper.toDto(userRepository.save(new User(userId, name, email)));
    }

    public UserDto getById(long userId) {
        return UserMapper.toDto(findUser(userId));
    }

    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toDto).toList();
    }

    public synchronized void delete(long userId) {
        findUser(userId);
        userRepository.deleteById(userId);
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void checkEmail(String email, Long userId) {
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ConflictException("Email is already in use: " + email);
        }
    }
}
