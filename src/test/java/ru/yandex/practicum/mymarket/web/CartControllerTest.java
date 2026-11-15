package ru.yandex.practicum.mymarket.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartDto;
import ru.yandex.practicum.mymarket.cart.CartService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CartService cart;

    @Test
    void emptyCartRendersCartTemplate() throws Exception {
        when(cart.getCart()).thenReturn(new CartDto(List.of(), 0));

        mvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("total", 0L))
                .andExpect(model().attributeExists("items"));
    }

    @Test
    void deleteActionIsDelegatedToCartService() throws Exception {
        when(cart.getCart()).thenReturn(new CartDto(List.of(), 0));

        mvc.perform(post("/cart/items").param("id", "42").param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
        verify(cart).change(42L, CartAction.DELETE);
    }
}
