package ru.yandex.practicum.mymarket.order;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class PurchaseOrderTest {
    @Test
    void preservesSnapshotIncludingImage() {
        var order = new PurchaseOrder();
        order.addLine(1L, "Мяч", 1800L, 2, "images/ball.svg");
        assertThat(order.getTotalSum()).isEqualTo(3600L);
        assertThat(order.getLines().getFirst().getImgPath()).isEqualTo("images/ball.svg");
    }

    @Test
    void rejectsInvalidQuantity() {
        var order = new PurchaseOrder();
        assertThatThrownBy(() -> order.addLine(1L, "Мяч", 1800L, 1001, "images/ball.svg"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(order.getLines()).isEmpty();
    }
}
