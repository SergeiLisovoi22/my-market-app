package ru.yandex.practicum.mymarket.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ru.yandex.practicum.mymarket.common.Money;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private long totalSum;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected PurchaseOrder() { }

    public void addLine(long itemId, String title, long price, int count, String imgPath) {
        long lineTotal = Money.lineTotal(price, count);
        long newTotal = Money.add(totalSum, lineTotal);
        lines.add(new OrderLine(this, itemId, title, price, count, imgPath));
        totalSum = newTotal;
    }
    public Long getId() { return id; }
    public long getTotalSum() { return totalSum; }
    public List<OrderLine> getLines() { return lines; }
    public Instant getCreatedAt() { return createdAt; }
}
