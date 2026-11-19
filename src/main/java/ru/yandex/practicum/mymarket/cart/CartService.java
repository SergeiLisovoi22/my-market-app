package ru.yandex.practicum.mymarket.cart;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.common.Money;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import ru.yandex.practicum.mymarket.item.ItemRepository;

@Service
public class CartService {
    private final CartLineRepository cartRepository;
    private final ItemRepository itemRepository;
    private final CartLockService lockService;

    public CartService(CartLineRepository cartRepository, ItemRepository itemRepository,
                       CartLockService lockService) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.lockService = lockService;
    }

    @Transactional(readOnly = true)
    public CartDto getCart() {
        var items = cartRepository.findAllWithItems().stream()
                .map(line -> ItemDto.from(line.getItem(), line.getCount()))
                .toList();
        long total = 0;
        for (ItemDto item : items) {
            total = Money.add(total, Money.lineTotal(item.price(), item.count()));
        }
        return new CartDto(items, total);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> quantities() {
        return cartRepository.findAllWithItems().stream()
                .collect(Collectors.toMap(c -> c.getItem().getId(), CartLine::getCount));
    }

    @Transactional
    public void change(long itemId, CartAction action) {
        // При любой операции сначала блокируем одну и ту же строку, даже если товара ещё нет.
        lockService.lock();
        var existing = cartRepository.findByItemId(itemId);
        if (action == CartAction.DELETE) {
            existing.ifPresent(cartRepository::delete);
            return;
        }
        if (action == CartAction.MINUS) {
            existing.ifPresent(line -> {
                if (line.getCount() <= 1) cartRepository.delete(line);
                else line.setCount(line.getCount() - 1);
            });
            return;
        }
        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден"));
        if (existing.isPresent()) {
            CartLine line = existing.get();
            if (line.getCount() >= Money.MAX_QUANTITY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Максимум " + Money.MAX_QUANTITY + " шт. одного товара");
            }
            Money.lineTotal(item.getPrice(), line.getCount() + 1);
            line.setCount(line.getCount() + 1);
        } else {
            Money.lineTotal(item.getPrice(), 1);
            cartRepository.save(new CartLine(item, 1));
        }
        // Проверяем и суммарную стоимость корзины. При ошибке транзакция откатится.
        getCart();
    }
}
