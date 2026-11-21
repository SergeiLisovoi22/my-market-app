package ru.yandex.practicum.mymarket.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.order.OrderService;

@Controller
public class OrdersController {
    private final OrderService orders;
    public OrdersController(OrderService orders) { this.orders = orders; }

    @GetMapping("/orders")
    public String list(Model model) {
        var allOrders = orders.list();
        model.addAttribute("orders", allOrders);
        model.addAttribute("totalOrdersSum", orders.totalOfOrders(allOrders));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable long id,
                         @RequestParam(defaultValue = "false") boolean newOrder,
                         Model model) {
        model.addAttribute("order", orders.get(id));
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    @PostMapping("/buy")
    public String buy() {
        long id = orders.checkout();
        return "redirect:/orders/" + id + "?newOrder=true";
    }
}
