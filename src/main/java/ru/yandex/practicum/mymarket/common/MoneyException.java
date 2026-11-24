package ru.yandex.practicum.mymarket.common;

/** Ошибка денежных вычислений, не зависящая от транспортного протокола. */
public class MoneyException extends IllegalArgumentException {
    public MoneyException(String message) { super(message); }
    public MoneyException(String message, Throwable cause) { super(message, cause); }
}
