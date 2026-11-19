package ru.yandex.practicum.mymarket.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.item.ItemPage;
import ru.yandex.practicum.mymarket.item.ItemService;

@Controller
public class ItemsController {
    private final ItemService items;
    private final CartService cart;

    public ItemsController(ItemService items, CartService cart) {
        this.items = items;
        this.cart = cart;
    }

    @GetMapping({"/", "/items"})
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "NO") String sort,
                       @RequestParam(defaultValue = "1") int pageNumber,
                       @RequestParam(defaultValue = "5") int pageSize,
                       Model model) {
        ItemPage page = items.list(search, sort, pageNumber, pageSize);
        model.addAttribute("items", page.items());
        model.addAttribute("search", page.search());
        model.addAttribute("sort", page.sort());
        model.addAttribute("paging", page.paging());
        return "items";
    }

    @PostMapping("/items")
    public String changeFromList(@RequestParam long id,
                                 @RequestParam CartAction action,
                                 @RequestParam(defaultValue = "") String search,
                                 @RequestParam(defaultValue = "NO") String sort,
                                 @RequestParam(defaultValue = "1") int pageNumber,
                                 @RequestParam(defaultValue = "5") int pageSize,
                                 RedirectAttributes redirect) {
        cart.change(id, action);
        redirect.addAttribute("search", search);
        redirect.addAttribute("sort", sort);
        redirect.addAttribute("pageNumber", pageNumber);
        redirect.addAttribute("pageSize", pageSize);
        return "redirect:/items";
    }

    @GetMapping("/items/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("item", items.get(id));
        return "item";
    }

    @PostMapping("/items/{id}")
    public String changeFromDetail(@PathVariable long id, @RequestParam CartAction action) {
        cart.change(id, action);
        return "redirect:/items/" + id;
    }
}
