package ru.yandex.practicum.mymarket.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartDto;
import ru.yandex.practicum.mymarket.cart.CartService;

@Controller
public class CartController {
    private final CartService cart;
    public CartController(CartService cart) { this.cart = cart; }

    @GetMapping({"/cart", "/cart/items"})
    public String show(Model model) {
        populate(model);
        return "cart";
    }

    @PostMapping("/cart/items")
    public String change(@RequestParam long id, @RequestParam CartAction action) {
        cart.change(id, action);
        return "redirect:/cart/items";
    }

    private void populate(Model model) {
        CartDto dto = cart.getCart();
        model.addAttribute("items", dto.items());
        model.addAttribute("total", dto.total());
    }
}
