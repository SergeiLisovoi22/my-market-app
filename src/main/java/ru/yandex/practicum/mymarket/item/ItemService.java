package ru.yandex.practicum.mymarket.item;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartService;

@Service
public class ItemService {
    private final ItemRepository items;
    private final CartService cart;

    public ItemService(ItemRepository items, CartService cart) {
        this.items = items;
        this.cart = cart;
    }

    public Mono<ItemPage> list(String search, String sort, int pageNumber, int pageSize) {
        String requestedSort = sort == null ? "NO" : sort.toUpperCase();
        String validSort = List.of("NO", "ALPHA", "PRICE").contains(requestedSort) ? requestedSort : "NO";
        int size = List.of(2, 5, 10, 20, 50, 100).contains(pageSize) ? pageSize : 5;
        int page = Math.max(1, pageNumber);
        long offset = ((long) page - 1L) * size;
        if (offset > Integer.MAX_VALUE) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Указан слишком большой номер страницы"));
        }
        String query = search == null ? "" : search.trim();
        Flux<ItemEntity> found = switch (validSort) {
            case "ALPHA" -> items.searchAlpha(query, size, offset);
            case "PRICE" -> items.searchPrice(query, size, offset);
            default -> items.searchDefault(query, size, offset);
        };
        return Mono.zip(found.collectList(), items.countMatching(query), cart.quantities())
                .map(tuple -> {
                    List<ItemDto> flat = tuple.getT1().stream()
                            .map(item -> ItemDto.from(item, tuple.getT3().getOrDefault(item.getId(), 0)))
                            .toList();
                    boolean previous = page > 1;
                    boolean next = offset + flat.size() < tuple.getT2() && !flat.isEmpty();
                    return new ItemPage(flat, query, validSort, new PagingDto(size, page, previous, next));
                });
    }

    public Mono<ItemDto> get(long id) {
        return Mono.zip(items.findById(id)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден"))),
                        cart.quantities())
                .map(tuple -> ItemDto.from(tuple.getT1(), tuple.getT2().getOrDefault(id, 0)));
    }
}
