package ru.yandex.practicum.mymarket.cart;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CartMutexRepository extends JpaRepository<CartMutex, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from CartMutex m where m.id = 1")
    Optional<CartMutex> lockCart();
}
