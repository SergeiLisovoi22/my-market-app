package ru.yandex.practicum.mymarket.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.item.ItemPage;
import ru.yandex.practicum.mymarket.item.ItemService;
import ru.yandex.practicum.mymarket.item.PagingDto;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemsController.class)
class ItemsControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ItemService items;
    @MockBean CartService cart;

    @Test
    void getItemsRendersRequiredModel() throws Exception {
        when(items.list("мяч", "PRICE", 1, 5))
                .thenReturn(new ItemPage(List.of(), "мяч", "PRICE", new PagingDto(5, 1, false, false)));
        mvc.perform(get("/items").param("search", "мяч").param("sort", "PRICE"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items", "search", "sort", "paging"));
    }

    @Test
    void postItemsPreservesSearchAndPageInRedirect() throws Exception {
        mvc.perform(post("/items").param("id", "3").param("action", "PLUS")
                        .param("search", "мяч").param("sort", "ALPHA")
                        .param("pageNumber", "2").param("pageSize", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/items?*"));
        verify(cart).change(3L, CartAction.PLUS);
    }

    @Test
    void postItemDetailRedirectsToGet() throws Exception {
        mvc.perform(post("/items/42").param("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/42"));
        verify(cart).change(42L, CartAction.PLUS);
    }
}
