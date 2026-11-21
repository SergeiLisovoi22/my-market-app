package ru.yandex.practicum.mymarket.order;

public record OrderItemDto(long id, String title, long price, int count, String imgPath) {
    public OrderItemDto(long id, String title, long price, int count) {
        this(id, title, price, count, "");
    }
}
