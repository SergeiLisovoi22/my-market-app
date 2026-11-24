package ru.yandex.practicum.mymarket.item;

import java.util.List;

public record ItemPage(List<ItemDto> items, String search, String sort, PagingDto paging) { }
