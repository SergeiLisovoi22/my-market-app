package ru.yandex.practicum.mymarket.order;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.cart.CartLockService;
import ru.yandex.practicum.mymarket.common.Money;
import ru.yandex.practicum.mymarket.item.ItemRepository;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OrderLineRepository orderLines;
    private final CartLineRepository cartLines;
    private final ItemRepository items;
    private final CartLockService lock;
    private final TransactionalOperator tx;

    public OrderService(OrderRepository orders, OrderLineRepository orderLines,
                        CartLineRepository cartLines, ItemRepository items,
                        CartLockService lock, TransactionalOperator tx) {
        this.orders = orders; this.orderLines = orderLines; this.cartLines = cartLines;
        this.items = items; this.lock = lock; this.tx = tx;
    }

    public Mono<Long> checkout() {
        return lock.lock()
                .then(cartLines.findAllByOrderByIdAsc().collectList())
                .flatMap(cart -> {
                    if (cart.isEmpty()) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Корзина пуста"));
                    return Flux.fromIterable(cart)
                            .concatMap(line -> items.findById(line.getItemId())
                                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден")))
                                    .map(item -> new OrderItemDto(item.getId(), item.getTitle(), item.getPrice(),
                                            line.getCount(), item.getImgPath())))
                            .collectList()
                            .flatMap(orderItems -> {
                                long total = 0;
                                for (OrderItemDto item : orderItems)
                                    total = Money.add(total, Money.lineTotal(item.price(), item.count()));
                                return orders.save(new PurchaseOrder(total))
                                        .flatMap(saved -> orderLines.saveAll(Flux.fromIterable(orderItems)
                                                .map(item -> new OrderLine(saved.getId(), item.id(), item.title(),
                                                        item.price(), item.count(), item.imgPath())))
                                                .then(cartLines.deleteAll())
                                                .thenReturn(saved.getId()));
                            });
                })
                .as(tx::transactional);
    }

    public Mono<List<OrderDto>> list() {
        return orders.findAllByOrderByIdDesc().concatMap(this::toDto).collectList();
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

    private Mono<OrderDto> toDto(PurchaseOrder order) {
        return orderLines.findByOrderIdOrderByIdAsc(order.getId())
                .map(line -> new OrderItemDto(line.getItemId(), line.getTitle(), line.getPrice(),
                        line.getCount(), line.getImgPath()))
                .collectList().map(items -> new OrderDto(order.getId(), items, order.getTotalSum()));
    }
}
