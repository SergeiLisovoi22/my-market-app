package ru.yandex.practicum.mymarket.cart;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.Money;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.item.ItemRepository;

@Service
public class CartService {
    private final CartLineRepository lines;
    private final ItemRepository items;
    private final CartLockService lock;
    private final TransactionalOperator tx;

    public CartService(CartLineRepository lines, ItemRepository items, CartLockService lock,
                       TransactionalOperator tx) {
        this.lines = lines; this.items = items; this.lock = lock; this.tx = tx;
    }

    public Mono<CartDto> getCart() {
        return lines.findAllByOrderByIdAsc().collectList()
                .flatMap(cartLines -> {
                    if (cartLines.isEmpty()) return Mono.just(new CartDto(List.of(), 0));
                    List<Long> ids = cartLines.stream().map(CartLine::getItemId).toList();
                    // Iterable overload: один SELECT ... IN (...), а не запрос для каждой позиции.
                    return items.findAllById(ids).collectMap(ItemEntity::getId)
                            .map(byId -> {
                                List<ItemDto> found = new ArrayList<>(cartLines.size());
                                long total = 0;
                                for (CartLine line : cartLines) {
                                    ItemEntity item = byId.get(line.getItemId());
                                    if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден");
                                    found.add(ItemDto.from(item, line.getCount()));
                                    total = Money.add(total, Money.lineTotal(item.getPrice(), line.getCount()));
                                }
                                return new CartDto(List.copyOf(found), total);
                            });
                });
    }

    public Mono<Map<Long, Integer>> quantities() {
        return lines.findAll().collectMap(CartLine::getItemId, CartLine::getCount);
    }

    public Mono<Void> change(long itemId, CartAction action) {
        return lock.lock()
                .then(lines.findByItemId(itemId)
                        .flatMap(line -> updateExisting(line, action).thenReturn(true))
                        .defaultIfEmpty(false)
                        .flatMap(existed -> !existed && action == CartAction.PLUS
                                ? addNew(itemId) : Mono.empty()))
                // Если общая стоимость вышла за допустимые границы, откатить изменение.
                .then(getCart())
                .then()
                .as(tx::transactional);
    }

    private Mono<Void> updateExisting(CartLine line, CartAction action) {
        if (action == CartAction.DELETE || (action == CartAction.MINUS && line.getCount() <= 1)) {
            return lines.delete(line);
        }
        if (action == CartAction.MINUS) {
            line.setCount(line.getCount() - 1);
            return lines.save(line).then();
        }
        if (line.getCount() >= Money.MAX_QUANTITY) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Максимум " + Money.MAX_QUANTITY + " шт. одного товара"));
        }
        return items.findById(line.getItemId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден")))
                .flatMap(item -> {
                    Money.lineTotal(item.getPrice(), line.getCount() + 1);
                    line.setCount(line.getCount() + 1);
                    return lines.save(line);
                }).then();
    }

    private Mono<Void> addNew(long itemId) {
        return items.findById(itemId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден")))
                .flatMap(item -> {
                    Money.lineTotal(item.getPrice(), 1);
                    return lines.save(new CartLine(itemId, 1));
                }).then();
    }
}
