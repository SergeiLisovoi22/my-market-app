package ru.yandex.practicum.mymarket.cart;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Одна строка для сериализации всех операций над общей корзиной магазина. */
@Entity
@Table(name = "cart_mutex")
public class CartMutex {
    @Id
    private Long id;

    protected CartMutex() { }

    public CartMutex(Long id) { this.id = id; }

    public Long getId() { return id; }
}
