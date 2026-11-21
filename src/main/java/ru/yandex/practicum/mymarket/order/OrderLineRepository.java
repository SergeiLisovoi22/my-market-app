package ru.yandex.practicum.mymarket.order;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderLineRepository extends ReactiveCrudRepository<OrderLine, Long> {
    Flux<OrderLine> findByOrderIdOrderByIdAsc(long orderId);
}
