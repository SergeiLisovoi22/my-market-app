package ru.yandex.practicum.mymarket.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    @Test void addsAndMultiplies() {
        assertThat(Money.lineTotal(100, 3)).isEqualTo(300);
        assertThat(Money.add(300, 200)).isEqualTo(500);
    }
    @Test void rejectsInvalidPriceAndQuantity() {
        assertThatThrownBy(() -> Money.validatePrice(Money.MAX_PRICE + 1)).isInstanceOf(MoneyException.class);
        assertThatThrownBy(() -> Money.lineTotal(1, Money.MAX_QUANTITY + 1)).isInstanceOf(MoneyException.class);
    }
    @Test void rejectsOverflow() {
        assertThatThrownBy(() -> Money.add(Long.MAX_VALUE, 1)).isInstanceOf(MoneyException.class);
    }
}
