package ru.yandex.practicum.mymarket.common;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    @Test
    void detectsOverflowWhenAdding() {
        assertThatThrownBy(() -> Money.add(Long.MAX_VALUE, 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("слишком большая");
    }

    @Test
    void rejectsOutOfRangePrice() {
        assertThatThrownBy(() -> Money.validatePrice(Money.MAX_PRICE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
