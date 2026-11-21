package ru.yandex.practicum.mymarket.cart;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_lines")
public class CartLine {
    @Id private Long id;
    @Column("item_id") private long itemId;
    private int count;

    public CartLine() { }
    public CartLine(long itemId, int count) { this.itemId = itemId; this.count = count; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getItemId() { return itemId; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
