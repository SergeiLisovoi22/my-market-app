package ru.yandex.practicum.mymarket.web;

import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.cart.CartAction;

final class FormValues {
    private FormValues() { }
    static String string(MultiValueMap<String, String> form, String key, String defaultValue) {
        String value = form.getFirst(key);
        return value == null ? defaultValue : value;
    }
    static int integer(MultiValueMap<String, String> form, String key, int defaultValue) {
        try { return Integer.parseInt(string(form, key, String.valueOf(defaultValue))); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный параметр " + key); }
    }
    static long longValue(MultiValueMap<String, String> form, String key) {
        try { return Long.parseLong(string(form, key, "")); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный параметр " + key); }
    }
    static CartAction action(MultiValueMap<String, String> form) {
        try { return CartAction.valueOf(string(form, "action", "")); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестное действие с товаром"); }
    }
}
