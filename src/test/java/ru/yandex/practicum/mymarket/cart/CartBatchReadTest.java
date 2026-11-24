package ru.yandex.practicum.mymarket.cart;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.item.ItemDto;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartBatchReadTest {
    @Test void loadsProductsInOneBatchAndPreservesCartOrder() {
        CartLineRepository lines = mock(CartLineRepository.class);
        ItemRepository items = mock(ItemRepository.class);
        CartService service = new CartService(lines, items, mock(CartLockService.class), mock(TransactionalOperator.class));
        ItemEntity first = new ItemEntity("Мяч", "Описание", "images/ball.svg", 100);
        first.setId(10L);
        ItemEntity second = new ItemEntity("Кепка", "Описание", "images/cap-red.svg", 250);
        second.setId(20L);
        when(lines.findAllByOrderByIdAsc()).thenReturn(Flux.just(new CartLine(10L, 2), new CartLine(20L, 3)));
        when(items.findAllById(List.of(10L, 20L))).thenReturn(Flux.just(second, first));

        StepVerifier.create(service.getCart()).assertNext(cart -> {
            assertThat(cart.total()).isEqualTo(950);
            assertThat(cart.items()).extracting(ItemDto::id).containsExactly(10L, 20L);
            assertThat(cart.items()).extracting(ItemDto::count).containsExactly(2, 3);
        }).verifyComplete();
        verify(lines).findAllByOrderByIdAsc();
        verify(items).findAllById(List.of(10L, 20L));
        verifyNoMoreInteractions(items);
    }

    @Test void emptyCartDoesNotQueryProducts() {
        CartLineRepository lines = mock(CartLineRepository.class);
        ItemRepository items = mock(ItemRepository.class);
        CartService service = new CartService(lines, items, mock(CartLockService.class), mock(TransactionalOperator.class));
        when(lines.findAllByOrderByIdAsc()).thenReturn(Flux.empty());
        StepVerifier.create(service.getCart()).assertNext(cart -> {
            assertThat(cart.items()).isEmpty();
            assertThat(cart.total()).isZero();
        }).verifyComplete();
        verifyNoInteractions(items);
    }
}
