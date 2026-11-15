package ru.yandex.practicum.mymarket.cart;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import ru.yandex.practicum.mymarket.item.ItemRepository;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class CartServiceTest {
    private CartLineRepository cartRepository;
    private ItemRepository itemRepository;
    private CartService service;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartLineRepository.class);
        itemRepository = mock(ItemRepository.class);
        service = new CartService(cartRepository, itemRepository);
    }

    @Test
    void plusAddsLineToEmptyCart() {
        var item = new ItemEntity("Мяч", "", "images/ball.svg", 100);
        when(cartRepository.findByItemId(1)).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        service.change(1L, CartAction.PLUS);
        verify(cartRepository).save(argThat(line -> line.getCount() == 1));
    }

    @Test
    void minusDeletesLastUnit() {
        var line = new CartLine(new ItemEntity("Мяч", "", "images/ball.svg", 100), 1);
        when(cartRepository.findByItemId(1)).thenReturn(Optional.of(line));
        service.change(1L, CartAction.MINUS);
        verify(cartRepository).delete(line);
    }

    @Test
    void deleteRemovesLine() {
        var line = new CartLine(new ItemEntity("Мяч", "", "images/ball.svg", 100), 3);
        when(cartRepository.findByItemId(1)).thenReturn(Optional.of(line));
        service.change(1L, CartAction.DELETE);
        verify(cartRepository).delete(line);
    }

    @Test
    void plusIncreasesQuantityOfExistingItem() {
        var line = new CartLine(new ItemEntity("Мяч", "", "images/ball.svg", 100), 2);
        when(cartRepository.findByItemId(1L)).thenReturn(Optional.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(line.getItem()));

        service.change(1L, CartAction.PLUS);

        assertThat(line.getCount()).isEqualTo(3);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void minusDecreasesQuantityWithoutRemovingLine() {
        var line = new CartLine(new ItemEntity("Мяч", "", "images/ball.svg", 100), 2);
        when(cartRepository.findByItemId(1L)).thenReturn(Optional.of(line));

        service.change(1L, CartAction.MINUS);

        assertThat(line.getCount()).isEqualTo(1);
        verify(cartRepository, never()).delete(line);
    }

}
