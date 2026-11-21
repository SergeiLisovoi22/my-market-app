package ru.yandex.practicum.mymarket.item;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import ru.yandex.practicum.mymarket.common.Money;

@Table("items")
public class ItemEntity {
    public static final int TITLE_MAX = 255;
    public static final int DESCRIPTION_MAX = 2000;
    public static final int IMAGE_PATH_MAX = 255;

    @Id private Long id;
    private String title;
    private String description;
    @Column("img_path") private String imgPath;
    private long price;

    public ItemEntity() { }
    public ItemEntity(String title, String description, String imgPath, long price) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX)
            throw new IllegalArgumentException("Название должно содержать 1–" + TITLE_MAX + " символов");
        if (description != null && description.length() > DESCRIPTION_MAX)
            throw new IllegalArgumentException("Описание слишком длинное");
        if (imgPath != null && imgPath.length() > IMAGE_PATH_MAX)
            throw new IllegalArgumentException("Путь к изображению слишком длинный");
        Money.validatePrice(price);
        this.title = title;
        this.description = description == null ? "" : description;
        this.imgPath = imgPath == null ? "" : imgPath;
        this.price = price;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImgPath() { return imgPath; }
    public long getPrice() { return price; }
}
