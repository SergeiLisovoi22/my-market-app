package ru.yandex.practicum.mymarket.order;

import java.util.List;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderLineRepository extends ReactiveCrudRepository<OrderLine, Long> {
    Flux<OrderLine> findByOrderIdOrderByIdAsc(long orderId);

    @Query("SELECT * FROM order_lines WHERE order_id IN (:orderIds) ORDER BY id ASC")
    Flux<OrderLine> findByOrderIds(@Param("orderIds") List<Long> orderIds);
}
