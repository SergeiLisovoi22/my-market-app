package ru.yandex.practicum.mymarket.order;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartLineRepository cartRepository;

    public OrderService(OrderRepository orderRepository, CartLineRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public long checkout() {
        var cartLines = cartRepository.findAllWithItems();
        if (cartLines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Корзина пуста");
        }
        PurchaseOrder order = new PurchaseOrder();
        for (var line : cartLines) {
            var item = line.getItem();
            order.addLine(item.getId(), item.getTitle(), item.getPrice(), line.getCount());
        }
        PurchaseOrder saved = orderRepository.save(order);
        cartRepository.deleteAll(cartLines);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> list() {
        return orderRepository.findAllByOrderByIdDesc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(long id) {
        return orderRepository.findById(id).map(this::map)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));
    }

    private OrderDto map(PurchaseOrder order) {
        var items = order.getLines().stream()
                .map(line -> new OrderItemDto(line.getItemId(), line.getTitle(), line.getPrice(), line.getCount()))
                .toList();
        return new OrderDto(order.getId(), items, order.getTotalSum());
    }
}
