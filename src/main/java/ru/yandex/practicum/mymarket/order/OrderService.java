package ru.yandex.practicum.mymarket.order;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.cart.CartLockService;
import ru.yandex.practicum.mymarket.common.Money;
import ru.yandex.practicum.mymarket.cart.CartService;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OrderLineRepository orderLines;
    private final CartLineRepository cartLines;
    private final CartService cart;
    private final CartLockService lock;
    private final TransactionalOperator tx;

    public OrderService(OrderRepository orders, OrderLineRepository orderLines,
                        CartLineRepository cartLines, CartService cart,
                        CartLockService lock, TransactionalOperator tx) {
        this.orders = orders; this.orderLines = orderLines; this.cartLines = cartLines;
        this.cart = cart; this.lock = lock; this.tx = tx;
    }

    public Mono<Long> checkout() {
        return lock.lock()
                .then(cart.getCart())
                .flatMap(snapshot -> {
                    if (snapshot.items().isEmpty()) return Mono.error(new EmptyCartException());
                    return orders.save(new PurchaseOrder(snapshot.total()))
                            .flatMap(saved -> orderLines.saveAll(Flux.fromIterable(snapshot.items())
                                    .map(item -> new OrderLine(saved.getId(), item.id(), item.title(),
                                            item.price(), item.count(), item.imgPath())))
                                    .then(cartLines.deleteAll())
                                    .thenReturn(saved.getId()));
                })
                .as(tx::transactional);
    }

    public Mono<List<OrderDto>> list() {
        return orders.findAllByOrderByIdDesc().collectList()
                .flatMap(all -> {
                    if (all.isEmpty()) return Mono.just(List.of());
                    List<Long> ids = all.stream().map(PurchaseOrder::getId).toList();
                    return orderLines.findByOrderIds(ids).collectList()
                            .map(lines -> {
                                Map<Long, List<OrderItemDto>> grouped = lines.stream().collect(Collectors.groupingBy(
                                        OrderLine::getOrderId, Collectors.mapping(OrderService::toItem, Collectors.toList())));
                                return all.stream().map(order -> new OrderDto(order.getId(),
                                        List.copyOf(grouped.getOrDefault(order.getId(), List.of())), order.getTotalSum())).toList();
                            });
                });
    }

    public long totalOfOrders(List<OrderDto> all) {
        long total = 0;
        for (OrderDto order : all) total = Money.add(total, order.totalSum());
        return total;
    }

    public Mono<OrderDto> get(long id) {
        return orders.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден")))
                .flatMap(this::toDto);
    }

    private static OrderItemDto toItem(OrderLine line) {
        return new OrderItemDto(line.getItemId(), line.getTitle(), line.getPrice(), line.getCount(), line.getImgPath());
    }

    private Mono<OrderDto> toDto(PurchaseOrder order) {
        return orderLines.findByOrderIdOrderByIdAsc(order.getId())
                .map(OrderService::toItem)
                .collectList().map(items -> new OrderDto(order.getId(), items, order.getTotalSum()));
    }
}
