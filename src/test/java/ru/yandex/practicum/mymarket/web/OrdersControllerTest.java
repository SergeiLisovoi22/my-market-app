package ru.yandex.practicum.mymarket.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.order.OrderDto;
import ru.yandex.practicum.mymarket.order.OrderItemDto;
import ru.yandex.practicum.mymarket.order.OrderService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdersController.class)
class OrdersControllerTest {
    @Autowired MockMvc mvc;
    @MockBean OrderService orders;

    @Test
    void buyingRedirectsToNewOrderPage() throws Exception {
        when(orders.checkout()).thenReturn(7L);

        mvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/7?newOrder=true"));
        verify(orders).checkout();
    }

    @Test
    void orderDetailsHaveExpectedModel() throws Exception {
        when(orders.get(7L)).thenReturn(new OrderDto(7L,
                List.of(new OrderItemDto(1L, "Мяч", 1800L, 2)), 3600L));

        mvc.perform(get("/orders/7").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(model().attributeExists("order"));
    }
}
