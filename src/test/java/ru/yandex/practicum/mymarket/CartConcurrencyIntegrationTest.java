package ru.yandex.practicum.mymarket;

import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.cart.*;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.order.OrderRepository;
import ru.yandex.practicum.mymarket.order.OrderService;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:market_concurrency;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CartConcurrencyIntegrationTest {
    @Autowired CartService cart;
    @Autowired OrderService checkout;
    @Autowired ItemRepository items;
    @Autowired OrderRepository orders;
    @Autowired CartLineRepository cartLines;

    private ExecutorService pool;

    @BeforeEach
    void prepare() {
        cartLines.deleteAll();
        orders.deleteAll();
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void stop() { pool.shutdownNow(); }

    private long itemId() { return items.findAll().getFirst().getId(); }

    private <T> Future<T> launch(CountDownLatch ready, CountDownLatch start, Callable<T> action) {
        return pool.submit(() -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Нет сигнала старта");
            return action.call();
        });
    }

    @Test
    void simultaneousAddsAreNotLost() throws Exception {
        long id = itemId();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var first = launch(ready, start, () -> { cart.change(id, CartAction.PLUS); return true; });
        var second = launch(ready, start, () -> { cart.change(id, CartAction.PLUS); return true; });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(first.get(15, TimeUnit.SECONDS)).isTrue();
        assertThat(second.get(15, TimeUnit.SECONDS)).isTrue();
        assertThat(cart.quantities()).containsEntry(id, 2);
    }

    @Test
    void simultaneousCheckoutsCreateOnlyOneOrder() throws Exception {
        long id = itemId();
        cart.change(id, CartAction.PLUS);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> action = () -> {
            try {
                checkout.checkout();
                return true;
            } catch (ResponseStatusException e) {
                if (e.getStatusCode() != HttpStatus.BAD_REQUEST) throw e;
                return false;
            }
        };
        var a = launch(ready, start, action);
        var b = launch(ready, start, action);
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        int successful = (a.get(15, TimeUnit.SECONDS) ? 1 : 0)
                + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
        assertThat(successful).isEqualTo(1);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(cartLines.count()).isZero();
    }

    @Test
    void checkoutAndAddDoNotLoseItem() throws Exception {
        long id = itemId();
        cart.change(id, CartAction.PLUS);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var a = launch(ready, start, () -> { checkout.checkout(); return true; });
        var b = launch(ready, start, () -> { cart.change(id, CartAction.PLUS); return true; });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(a.get(15, TimeUnit.SECONDS)).isTrue();
        assertThat(b.get(15, TimeUnit.SECONDS)).isTrue();
        var orderId = orders.findAll().getFirst().getId();
        long quantityInOrder = checkout.get(orderId).items().stream().mapToLong(x -> x.count()).sum();
        long quantityInCart = cart.quantities().getOrDefault(id, 0);
        assertThat(quantityInOrder + quantityInCart).isEqualTo(2);
    }
}
