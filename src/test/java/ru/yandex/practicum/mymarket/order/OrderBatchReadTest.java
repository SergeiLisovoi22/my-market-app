package ru.yandex.practicum.mymarket.order;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.cart.CartLockService;
import ru.yandex.practicum.mymarket.cart.CartService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderBatchReadTest {
    @Test void loadsLinesInOneBatchAndGroupsByOrder() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderLineRepository lines = mock(OrderLineRepository.class);
        OrderService service = new OrderService(orders, lines, mock(CartLineRepository.class),
                mock(CartService.class), mock(CartLockService.class), mock(TransactionalOperator.class));
        PurchaseOrder older = new PurchaseOrder(200);
        older.setId(1L);
        PurchaseOrder newer = new PurchaseOrder(650);
        newer.setId(2L);
        when(orders.findAllByOrderByIdDesc()).thenReturn(Flux.just(newer, older));
        when(lines.findByOrderIds(List.of(2L, 1L))).thenReturn(Flux.just(
                new OrderLine(1L, 10L, "Мяч", 100, 2, "images/ball.svg"),
                new OrderLine(2L, 20L, "Кепка", 250, 1, "images/cap-red.svg"),
                new OrderLine(2L, 30L, "Зонт", 200, 2, "images/umbrella.svg")));
        StepVerifier.create(service.list()).assertNext(all -> {
            assertThat(all).extracting(OrderDto::id).containsExactly(2L, 1L);
            assertThat(all.get(0).items()).extracting(OrderItemDto::id).containsExactly(20L, 30L);
            assertThat(all.get(1).items()).extracting(OrderItemDto::id).containsExactly(10L);
            assertThat(service.totalOfOrders(all)).isEqualTo(850);
        }).verifyComplete();
        verify(orders).findAllByOrderByIdDesc();
        verify(lines).findByOrderIds(List.of(2L, 1L));
        verifyNoMoreInteractions(lines);
    }

    @Test void emptyOrderListDoesNotQueryPositions() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderLineRepository lines = mock(OrderLineRepository.class);
        OrderService service = new OrderService(orders, lines, mock(CartLineRepository.class),
                mock(CartService.class), mock(CartLockService.class), mock(TransactionalOperator.class));
        when(orders.findAllByOrderByIdDesc()).thenReturn(Flux.empty());
        StepVerifier.create(service.list()).assertNext(all -> assertThat(all).isEmpty()).verifyComplete();
        verifyNoInteractions(lines);
    }
}
