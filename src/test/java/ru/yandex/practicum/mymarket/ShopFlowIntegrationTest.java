package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // все изменения в тестовых сценариях откатываются Spring TestContext
class ShopFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ItemRepository items;
    @Autowired CartLineRepository cart;
    @Autowired OrderRepository orders;

    @Test
    void checkoutCreatesOrderAndClearsCart() throws Exception {
        long itemId = items.findAll().getFirst().getId();
        long previousOrders = orders.count();
        mvc.perform(post("/items").param("id", String.valueOf(itemId)).param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("items", "total"));
        mvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("newOrder=true")));
        assertThat(orders.count()).isEqualTo(previousOrders + 1);
        assertThat(cart.count()).isZero();
    }

    @Test
    void pagesRenderUsingSkeletonTemplates() throws Exception {
        mvc.perform(get("/items")).andExpect(status().isOk()).andExpect(view().name("items"));
        long itemId = items.findAll().getFirst().getId();
        mvc.perform(get("/items/{id}", itemId)).andExpect(status().isOk()).andExpect(view().name("item"));
        mvc.perform(get("/orders")).andExpect(status().isOk()).andExpect(view().name("orders"));
        mvc.perform(get("/images/ball.svg")).andExpect(status().isOk());
    }

    @Test
    void checkoutWithMultipleItemsPreservesPricesAndCounts() throws Exception {
        var catalog = items.findAll(org.springframework.data.domain.Sort.by("id"));
        var first = catalog.get(0);
        var second = catalog.get(1);
        long previousOrders = orders.count();

        mvc.perform(post("/items").param("id", first.getId().toString()).param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/items").param("id", first.getId().toString()).param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/items").param("id", second.getId().toString()).param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/buy")).andExpect(status().is3xxRedirection());

        assertThat(orders.count()).isEqualTo(previousOrders + 1);
        var order = orders.findAllByOrderByIdDesc().getFirst();
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines()).extracting("imgPath")
                .contains(first.getImgPath(), second.getImgPath());
        assertThat(order.getTotalSum()).isEqualTo(first.getPrice() * 2 + second.getPrice());
        assertThat(cart.count()).isZero();
    }

    @Test
    void invalidCsvDoesNotChangeCatalogAndShowsError() throws Exception {
        long before = items.count();
        String csv = "title;description;price;imgPath\n"
                + "Хороший;Описание;100;images/ball.svg\n"
                + "X".repeat(256) + ";Описание;100;images/ball.svg\n";
        var file = new org.springframework.mock.web.MockMultipartFile("file", "catalog.csv", "text/csv",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mvc.perform(multipart("/admin/items/import").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/items/import"))
                .andExpect(flash().attributeExists("error"));
        assertThat(items.count()).isEqualTo(before);
    }

    @Test
    void checkoutWithEmptyCartReturnsBadRequest() throws Exception {
        long previousOrders = orders.count();
        assertThat(cart.count()).isZero();

        mvc.perform(post("/buy"))
                .andExpect(status().isBadRequest());

        assertThat(orders.count()).isEqualTo(previousOrders);
    }

}
