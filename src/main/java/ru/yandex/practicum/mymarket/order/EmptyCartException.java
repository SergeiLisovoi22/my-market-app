package ru.yandex.practicum.mymarket.order;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException() { super("Корзина пуста"); }
}
