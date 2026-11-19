package ru.yandex.practicum.mymarket.order;

import jakarta.persistence.*;

@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;
    @Column(nullable = false)
    private long itemId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private long price;
    @Column(nullable = false)
    private int count;
    // nullable для совместимости с заказами, созданными до добавления этого поля.
    @Column(name = "img_path", length = 255)
    private String imgPath;

    protected OrderLine() { }

    public OrderLine(PurchaseOrder order, long itemId, String title, long price, int count, String imgPath) {
        this.order = order;
        this.itemId = itemId;
        this.title = title;
        this.price = price;
        this.count = count;
        this.imgPath = imgPath;
    }
    public long getItemId() { return itemId; }
    public String getTitle() { return title; }
    public long getPrice() { return price; }
    public int getCount() { return count; }
    public String getImgPath() { return imgPath; }
}
