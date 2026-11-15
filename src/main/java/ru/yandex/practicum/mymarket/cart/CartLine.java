package ru.yandex.practicum.mymarket.cart;

import jakarta.persistence.*;
import ru.yandex.practicum.mymarket.item.ItemEntity;

@Entity
@Table(name = "cart_lines", uniqueConstraints = @UniqueConstraint(columnNames = "item_id"))
public class CartLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEntity item;
    @Column(nullable = false)
    private int count;

    protected CartLine() { }

    public CartLine(ItemEntity item, int count) {
        this.item = item;
        this.count = count;
    }
    public Long getId() { return id; }
    public ItemEntity getItem() { return item; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
