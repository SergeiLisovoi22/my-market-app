package ru.yandex.practicum.mymarket.web;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.buffer.DataBufferLimitException;
import ru.yandex.practicum.mymarket.common.MoneyException;
import ru.yandex.practicum.mymarket.order.EmptyCartException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Показываем пользователю причину отказа вместо безымянной страницы HTTP 400. */
@ControllerAdvice
public class BusinessErrorHandler {
    @ExceptionHandler(MoneyException.class)
    public ResponseEntity<String> onMoneyError(MoneyException error) {
        return onBusinessError(new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()));
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<String> onEmptyCart(EmptyCartException error) {
        return onBusinessError(new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()));
    }

    // Защита также на случай ошибки декодирования multipart до вызова контроллера.
    @ExceptionHandler(DataBufferLimitException.class)
    public ResponseEntity<String> onBufferLimit(DataBufferLimitException error) {
        return onBusinessError(new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                "Превышен допустимый размер файла или запроса"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> onBusinessError(ResponseStatusException error) {
        String reason = error.getReason() == null ? "Невозможно выполнить операцию" : error.getReason();
        return ResponseEntity.status(error.getStatusCode())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body("Ошибка: " + reason);
    }
}
