package ru.yandex.practicum.mymarket.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.item.ItemService;

@Controller
public class ItemsController {
    private final ItemService items;
    private final CartService cart;
    public ItemsController(ItemService items, CartService cart) { this.items = items; this.cart = cart; }

    @GetMapping({"/", "/items"})
    public Mono<String> list(@RequestParam(defaultValue = "") String search,
                             @RequestParam(defaultValue = "NO") String sort,
                             @RequestParam(defaultValue = "1") int pageNumber,
                             @RequestParam(defaultValue = "5") int pageSize,
                             Model model) {
        return items.list(search, sort, pageNumber, pageSize)
                .map(page -> {
                    model.addAttribute("items", page.items());
                    model.addAttribute("search", page.search());
                    model.addAttribute("sort", page.sort());
                    model.addAttribute("paging", page.paging());
                    return "items";
                });
    }

    // WebFlux @RequestParam не читает поля HTML POST формы: используем getFormData().
    @PostMapping("/items")
    public Mono<String> changeFromList(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(form -> {
            long id = FormValues.longValue(form, "id");
            var action = FormValues.action(form);
            String search = FormValues.string(form, "search", "");
            String sort = FormValues.string(form, "sort", "NO");
            int pageNumber = FormValues.integer(form, "pageNumber", 1);
            int pageSize = FormValues.integer(form, "pageSize", 5);
            String url = UriComponentsBuilder.fromPath("/items")
                    .queryParam("search", search).queryParam("sort", sort)
                    .queryParam("pageNumber", pageNumber).queryParam("pageSize", pageSize)
                    .build().encode().toUriString();
            return cart.change(id, action).thenReturn("redirect:" + url);
        });
    }

    @GetMapping("/items/{id}")
    public Mono<String> detail(@PathVariable long id, Model model) {
        return items.get(id).map(item -> {
            model.addAttribute("item", item);
            return "item";
        });
    }

    @PostMapping("/items/{id}")
    public Mono<String> changeFromDetail(@PathVariable long id, ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> cart.change(id, FormValues.action(form)))
                .thenReturn("redirect:/items/" + id);
    }
}
