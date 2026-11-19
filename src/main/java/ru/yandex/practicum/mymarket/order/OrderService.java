package ru.yandex.practicum.mymarket.order;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.cart.CartLockService;
import ru.yandex.practicum.mymarket.common.Money;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartLineRepository cartRepository;
    private final CartLockService lockService;

    public OrderService(OrderRepository orderRepository, CartLineRepository cartRepository,
                        CartLockService lockService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.lockService = lockService;
    }

    @Transactional
    public long checkout() {
        // Используем ту же блокировку, что и изменение корзины. Иначе две покупки
        // могут одновременно прочитать одни и те же товары.
        lockService.lock();
        var cartLines = cartRepository.findAllWithItems();
        if (cartLines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Корзина пуста");
        }
        PurchaseOrder order = new PurchaseOrder();
        for (var line : cartLines) {
            var item = line.getItem();
            order.addLine(item.getId(), item.getTitle(), item.getPrice(),
                    line.getCount(), item.getImgPath());
        }
        PurchaseOrder saved = orderRepository.save(order);
        cartRepository.deleteAll(cartLines);
        // Записи корзины должны быть удалены до освобождения блокировки.
        cartRepository.flush();
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> list() {
        return orderRepository.findAllByOrderByIdDesc().stream().map(this::map).toList();
    }

    public long totalOfOrders(List<OrderDto> orders) {
        long total = 0;
        for (OrderDto order : orders) total = Money.add(total, order.totalSum());
        return total;
    }

    @Transactional(readOnly = true)
    public OrderDto get(long id) {
        return orderRepository.findById(id).map(this::map)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));
    }

    private OrderDto map(PurchaseOrder order) {
        var items = order.getLines().stream()
                .map(line -> new OrderItemDto(line.getItemId(), line.getTitle(),
                        line.getPrice(), line.getCount(), line.getImgPath()))
                .toList();
        return new OrderDto(order.getId(), items, order.getTotalSum());
    }
}
