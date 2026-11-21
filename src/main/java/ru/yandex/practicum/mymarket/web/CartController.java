package ru.yandex.practicum.mymarket.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartService;

@Controller
public class CartController {
    private final CartService cart;
    public CartController(CartService cart) { this.cart = cart; }

    @GetMapping({"/cart", "/cart/items"})
    public Mono<String> show(Model model) {
        return cart.getCart().map(dto -> {
            model.addAttribute("items", dto.items());
            model.addAttribute("total", dto.total());
            return "cart";
        });
    }

    @PostMapping("/cart/items")
    public Mono<String> change(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> cart.change(FormValues.longValue(form, "id"), FormValues.action(form)))
                .thenReturn("redirect:/cart/items");
    }
}
