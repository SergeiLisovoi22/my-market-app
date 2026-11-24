package ru.yandex.practicum.mymarket.cart;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CartLockService {
    private final DatabaseClient client;
    public CartLockService(DatabaseClient client) { this.client = client; }

    /** Вызывается внутри реактивной транзакции, перед чтением/изменением корзины. */
    public Mono<Void> lock() {
        return client.sql("SELECT id FROM cart_mutex WHERE id = 1 FOR UPDATE")
                .map((row, metadata) -> row.get("id", Long.class))
                .one()
                .switchIfEmpty(Mono.error(new IllegalStateException("Не найдена строка блокировки корзины")))
                .then();
    }
}
