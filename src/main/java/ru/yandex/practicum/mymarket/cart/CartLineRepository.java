package ru.yandex.practicum.mymarket.cart;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartLineRepository extends JpaRepository<CartLine, Long> {
    @Query("select c from CartLine c join fetch c.item order by c.id")
    List<CartLine> findAllWithItems();

    @Query("select c from CartLine c where c.item.id = :itemId")
    Optional<CartLine> findByItemId(@Param("itemId") long itemId);
}
