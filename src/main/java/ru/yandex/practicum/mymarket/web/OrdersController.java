package ru.yandex.practicum.mymarket.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.order.OrderService;

@Controller
public class OrdersController {
    private final OrderService orders;
    public OrdersController(OrderService orders) { this.orders = orders; }

    @GetMapping("/orders")
    public Mono<String> list(Model model) {
        return orders.list().map(all -> {
            model.addAttribute("orders", all);
            model.addAttribute("totalOrdersSum", orders.totalOfOrders(all));
            return "orders";
        });
    }

    @GetMapping("/orders/{id}")
    public Mono<String> detail(@PathVariable long id,
                               @RequestParam(defaultValue = "false") boolean newOrder,
                               Model model) {
        return orders.get(id).map(order -> {
            model.addAttribute("order", order);
            model.addAttribute("newOrder", newOrder);
            return "order";
        });
    }

    @PostMapping("/buy")
    public Mono<String> buy() {
        return orders.checkout().map(id -> "redirect:/orders/" + id + "?newOrder=true");
    }
}
