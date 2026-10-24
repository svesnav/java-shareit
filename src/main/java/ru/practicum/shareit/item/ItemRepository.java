package ru.practicum.shareit.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByOwnerIdOrderByIdAsc(long ownerId);

    @Query("select i from Item i where i.available = true and "
            + "(locate(lower(:text), lower(i.name)) > 0 or locate(lower(:text), lower(i.description)) > 0) "
            + "order by i.id")
    List<Item> searchAvailable(@Param("text") String text);
}
