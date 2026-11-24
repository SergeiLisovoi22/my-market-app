package ru.yandex.practicum.mymarket.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.yandex.practicum.mymarket.common.MoneyException;
import static org.assertj.core.api.Assertions.*;

class BusinessErrorHandlerTest {
    @Test void mapsDomainMoneyErrorToBadRequest() {
        var response = new BusinessErrorHandler().onMoneyError(new MoneyException("Общая сумма слишком большая"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Общая сумма слишком большая");
    }
}
