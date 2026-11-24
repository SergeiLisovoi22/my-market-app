package ru.yandex.practicum.mymarket.item;

public record ItemDto(long id, String title, String description, String imgPath, long price, int count) {
    public static ItemDto from(ItemEntity item, int count) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count);
    }
}
