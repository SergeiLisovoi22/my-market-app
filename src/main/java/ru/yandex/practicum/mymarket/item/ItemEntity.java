package ru.yandex.practicum.mymarket.item;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class ItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(name = "img_path", nullable = false)
    private String imgPath;
    @Column(nullable = false)
    private long price;

    protected ItemEntity() { }

    public ItemEntity(String title, String description, String imgPath, long price) {
        if (title == null || title.isBlank() || price < 0) {
            throw new IllegalArgumentException("Название обязательно, цена не может быть отрицательной");
        }
        this.title = title;
        this.description = description == null ? "" : description;
        this.imgPath = imgPath == null ? "" : imgPath;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImgPath() { return imgPath; }
    public long getPrice() { return price; }
}
