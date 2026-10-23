package ru.practicum.shareit.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    @Override
    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findAll(Specification<Booking> specification, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findLockedById(@Param("id") long id);

    List<Booking> findByItemIdInAndStatusOrderByStartDescIdDesc(Collection<Long> itemIds, BookingStatus status);

    boolean existsByItemIdAndBookerIdAndStatusAndEndLessThanEqual(
            long itemId, long bookerId, BookingStatus status, LocalDateTime end);
}
