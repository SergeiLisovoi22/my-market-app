package ru.yandex.practicum.mymarket.common;

/** Денежные суммы хранятся в целых рублях, без округления. */
public final class Money {
    public static final long MAX_PRICE = 1_000_000_000L;
    public static final int MAX_QUANTITY = 1_000;

    private Money() { }

    public static void validatePrice(long price) {
        if (price < 0 || price > MAX_PRICE) {
            throw new MoneyException("Цена должна быть от 0 до " + MAX_PRICE + " руб.");
        }
    }

    public static long lineTotal(long price, int quantity) {
        if (price < 0 || price > MAX_PRICE || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new MoneyException("Недопустимая цена или количество товара");
        }
        try {
            return Math.multiplyExact(price, (long) quantity);
        } catch (ArithmeticException ex) {
            throw new MoneyException("Сумма слишком большая", ex);
        }
    }

    public static long add(long total, long amount) {
        try {
            return Math.addExact(total, amount);
        } catch (ArithmeticException ex) {
            throw new MoneyException("Общая сумма слишком большая", ex);
        }
    }
}
