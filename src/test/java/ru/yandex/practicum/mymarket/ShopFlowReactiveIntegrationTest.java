package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.item.CatalogImportService;
import ru.yandex.practicum.mymarket.item.ItemService;
import ru.yandex.practicum.mymarket.order.OrderLineRepository;
import ru.yandex.practicum.mymarket.order.OrderRepository;
import ru.yandex.practicum.mymarket.order.OrderService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ShopFlowReactiveIntegrationTest {
    @Autowired WebTestClient client;
    @Autowired CartService cart;
    @Autowired ItemService itemService;
    @Autowired OrderService orderService;
    @Autowired ItemRepository itemRepository;
    @Autowired CatalogImportService importer;
    @Autowired CartLineRepository cartRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderLineRepository orderLineRepository;

    @BeforeEach void clearCartAndOrders() {
        orderLineRepository.deleteAll().then(orderRepository.deleteAll())
                .then(cartRepository.deleteAll()).block();
    }

    private long itemId() { return itemRepository.findAll().next().block().getId(); }

    @Test void rendersCatalogAndDetail() {
        client.get().uri("/items").exchange().expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Витрина магазина"));
        client.get().uri("/items/" + itemId()).exchange().expectStatus().isOk();
    }

    @Test void supportsSearchAndPagination() {
        client.get().uri("/items?search=мяч&sort=PRICE&pageSize=2").exchange().expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Футбольный мяч"));
    }

    @Test void rejectsHugePageNumber() {
        client.get().uri("/items?pageNumber=100000000&pageSize=100")
                .exchange().expectStatus().isBadRequest();
    }

    @Test void allowsEmptyValidPage() {
        client.get().uri("/items?pageNumber=1000&pageSize=100")
                .exchange().expectStatus().isOk();
    }

    @Test void formPostUsesRedirectAndUpdatesCart() {
        long id = itemId();
        client.post().uri("/items").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", Long.toString(id))
                        .with("action", "PLUS").with("sort", "NO").with("pageSize", "5"))
                .exchange().expectStatus().is3xxRedirection();
        StepVerifier.create(cart.getCart())
                .assertNext(dto -> {
                    assertThat(dto.items()).hasSize(1);
                    assertThat(dto.items().get(0).count()).isEqualTo(1);
                }).verifyComplete();
    }

    @Test void checkoutPersistsOrderAndClearsCart() {
        long id = itemId();
        StepVerifier.create(cart.change(id, CartAction.PLUS).then(cart.change(id, CartAction.PLUS))
                .then(orderService.checkout())
                .flatMap(orderId -> orderService.get(orderId)))
                .assertNext(order -> {
                    assertThat(order.items()).hasSize(1);
                    assertThat(order.items().get(0).count()).isEqualTo(2);
                    assertThat(order.totalSum()).isPositive();
                }).verifyComplete();
        StepVerifier.create(cart.getCart()).assertNext(dto -> assertThat(dto.items()).isEmpty()).verifyComplete();
    }

    @Test void emptyCheckoutFails() {
        StepVerifier.create(orderService.checkout()).expectError().verify();
    }

    @Test void csvImportIsAtomicOnValidationError() {
        String bad = "title;description;price;imgPath\n"
                + "Новый товар;Описание;120;images/ball.svg\n"
                + "X".repeat(256) + ";Описание;100;images/ball.svg\n";
        long before = itemRepository.count().block();
        StepVerifier.create(importer.importText(bad))
                .expectError(IllegalArgumentException.class).verify();
        assertThat(itemRepository.count().block()).isEqualTo(before);
    }

    @Test void checkoutHttpRedirectsToOrder() {
        cart.change(itemId(), CartAction.PLUS).block();
        client.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*orders/[0-9]+\\?newOrder=true");
    }

    @Test void rendersOrdersAndCart() {
        client.get().uri("/cart/items").exchange().expectStatus().isOk();
        client.get().uri("/orders").exchange().expectStatus().isOk();
    }
}
