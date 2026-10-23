package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponseDto create(long userId, BookingDto dto) {
        LocalDateTime now = LocalDateTime.now();
        if (dto.getItemId() == null || dto.getItemId() <= 0 || dto.getStart() == null || dto.getEnd() == null
                || dto.getStart().isBefore(now) || !dto.getEnd().isAfter(dto.getStart())) {
            throw new IllegalArgumentException("Booking dates must be in the future and end must be after start");
        }
        User booker = findUser(userId);
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found: " + dto.getItemId()));
        if (item.getOwner().getId() == userId) {
            throw new IllegalArgumentException("Owner cannot book their own item");
        }
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException("Item is not available for booking");
        }
        Booking booking = new Booking(null, dto.getStart(), dto.getEnd(), item, booker, BookingStatus.WAITING);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponseDto approve(long userId, long bookingId, boolean approved) {
        Booking booking = bookingRepository.findLockedById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (booking.getItem().getOwner().getId() != userId) {
            throw new ForbiddenException("Only the item owner can approve or reject booking: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new IllegalArgumentException("Only a waiting booking can be approved or rejected");
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDto getById(long userId, long bookingId) {
        findUser(userId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (booking.getBooker().getId() != userId && booking.getItem().getOwner().getId() != userId) {
            throw new NotFoundException("Booking not found for user: " + userId);
        }
        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingResponseDto> getByBooker(long userId, BookingState state) {
        findUser(userId);
        Specification<Booking> byBooker = (root, query, builder) -> builder.equal(root.get("booker").get("id"), userId);
        return findBookings(byBooker, state);
    }

    @Override
    public List<BookingResponseDto> getByOwner(long userId, BookingState state) {
        findUser(userId);
        Specification<Booking> byOwner = (root, query, builder) ->
                builder.equal(root.get("item").get("owner").get("id"), userId);
        return findBookings(byOwner, state);
    }

    private List<BookingResponseDto> findBookings(Specification<Booking> participant, BookingState state) {
        LocalDateTime now = LocalDateTime.now();
        Specification<Booking> byState = (root, query, builder) -> switch (state) {
            case ALL -> builder.conjunction();
            case CURRENT -> builder.and(builder.lessThanOrEqualTo(root.get("start"), now),
                    builder.greaterThan(root.get("end"), now));
            case PAST -> builder.lessThanOrEqualTo(root.get("end"), now);
            case FUTURE -> builder.greaterThan(root.get("start"), now);
            case WAITING -> builder.equal(root.get("status"), BookingStatus.WAITING);
            case REJECTED -> builder.equal(root.get("status"), BookingStatus.REJECTED);
        };
        return bookingRepository.findAll(participant.and(byState), Sort.by(Sort.Direction.DESC, "start", "id"))
                .stream().map(BookingMapper::toDto).toList();
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
