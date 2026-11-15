package ru.yandex.practicum.mymarket.item;

public record PagingDto(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) { }
