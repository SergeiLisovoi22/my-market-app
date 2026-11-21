package ru.yandex.practicum.mymarket.item;

import jakarta.persistence.*;
import ru.yandex.practicum.mymarket.common.Money;

@Entity
@Table(name = "items")
public class ItemEntity {
    public static final int TITLE_MAX = 255;
    public static final int DESCRIPTION_MAX = 2000;
    public static final int IMAGE_PATH_MAX = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = TITLE_MAX)
    private String title;
    @Column(nullable = false, length = DESCRIPTION_MAX)
    private String description;
    @Column(name = "img_path", nullable = false, length = IMAGE_PATH_MAX)
    private String imgPath;
    @Column(nullable = false)
    private long price;

    protected ItemEntity() { }

    public ItemEntity(String title, String description, String imgPath, long price) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX) {
            throw new IllegalArgumentException("Название должно содержать 1–" + TITLE_MAX + " символов");
        }
        if (description != null && description.length() > DESCRIPTION_MAX) {
            throw new IllegalArgumentException("Описание слишком длинное");
        }
        if (imgPath != null && imgPath.length() > IMAGE_PATH_MAX) {
            throw new IllegalArgumentException("Путь к изображению слишком длинный");
        }
        Money.validatePrice(price);
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
