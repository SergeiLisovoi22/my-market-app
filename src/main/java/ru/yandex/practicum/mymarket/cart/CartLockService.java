package ru.yandex.practicum.mymarket.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartLockService {
    private final CartMutexRepository repository;

    public CartLockService(CartMutexRepository repository) {
        this.repository = repository;
    }

    /** Вызывается внутри пишущей транзакции ДО чтения строк корзины. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock() {
        repository.lockCart().orElseThrow(() ->
                new IllegalStateException("Запись блокировки корзины не инициализирована"));
    }
}
