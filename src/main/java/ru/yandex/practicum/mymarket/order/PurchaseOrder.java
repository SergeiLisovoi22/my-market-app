package ru.yandex.practicum.mymarket.order;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("purchase_orders")
public class PurchaseOrder {
    @Id private Long id;
    @Column("total_sum") private long totalSum;
    @Column("created_at") private LocalDateTime createdAt;

    public PurchaseOrder() { }
    public PurchaseOrder(long totalSum) { this.totalSum = totalSum; this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getTotalSum() { return totalSum; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
