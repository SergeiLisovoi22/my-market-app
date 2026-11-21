package ru.yandex.practicum.mymarket.order;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_lines")
public class OrderLine {
    @Id private Long id;
    @Column("order_id") private long orderId;
    @Column("item_id") private long itemId;
    private String title;
    private long price;
    private int count;
    @Column("img_path") private String imgPath;

    public OrderLine() { }
    public OrderLine(long orderId, long itemId, String title, long price, int count, String imgPath) {
        this.orderId = orderId; this.itemId = itemId; this.title = title;
        this.price = price; this.count = count; this.imgPath = imgPath;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getOrderId() { return orderId; }
    public long getItemId() { return itemId; }
    public String getTitle() { return title; }
    public long getPrice() { return price; }
    public int getCount() { return count; }
    public String getImgPath() { return imgPath; }
}
