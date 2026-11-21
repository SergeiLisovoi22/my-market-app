package ru.yandex.practicum.mymarket.cart;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartLineRepository extends ReactiveCrudRepository<CartLine, Long> {
    Flux<CartLine> findAllByOrderByIdAsc();
    Mono<CartLine> findByItemId(long itemId);
}
