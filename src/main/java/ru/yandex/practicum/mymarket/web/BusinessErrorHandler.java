package ru.yandex.practicum.mymarket.web;

import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Показываем пользователю причину отказа вместо безымянной страницы HTTP 400. */
@ControllerAdvice
public class BusinessErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> onBusinessError(ResponseStatusException error) {
        String reason = error.getReason() == null ? "Невозможно выполнить операцию" : error.getReason();
        return ResponseEntity.status(error.getStatusCode())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body("Ошибка: " + reason);
    }
}
