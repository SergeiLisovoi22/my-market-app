package ru.yandex.practicum.mymarket.cart;

import java.util.List;
import ru.yandex.practicum.mymarket.item.ItemDto;

public record CartDto(List<ItemDto> items, long total) { }
