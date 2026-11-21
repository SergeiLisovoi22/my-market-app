package ru.yandex.practicum.mymarket.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.cart.CartService;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;

    public ItemService(ItemRepository itemRepository, CartService cartService) {
        this.itemRepository = itemRepository;
        this.cartService = cartService;
    }

    @Transactional(readOnly = true)
    public ItemPage list(String search, String sort, int pageNumber, int pageSize) {
        String requestedSort = sort == null ? "NO" : sort.toUpperCase();
        Sort ordering = switch (requestedSort) {
            case "ALPHA" -> Sort.by("title").ascending().and(Sort.by("id"));
            case "PRICE" -> Sort.by("price").ascending().and(Sort.by("id"));
            default -> Sort.by("id").ascending();
        };
        String validSort = List.of("NO", "ALPHA", "PRICE").contains(requestedSort) ? requestedSort : "NO";
        int size = List.of(2, 5, 10, 20, 50, 100).contains(pageSize) ? pageSize : 5;
        int page = Math.max(1, pageNumber);
        String query = search == null ? "" : search.trim();
        var result = itemRepository.search(query, PageRequest.of(page - 1, size, ordering));
        Map<Long, Integer> counts = cartService.quantities();
        List<ItemDto> flat = result.getContent().stream()
                .map(item -> ItemDto.from(item, counts.getOrDefault(item.getId(), 0)))
                .toList();
        List<List<ItemDto>> rows = new ArrayList<>();
        for (int i = 0; i < flat.size(); i += 3) {
            List<ItemDto> row = new ArrayList<>(flat.subList(i, Math.min(i + 3, flat.size())));
            while (row.size() < 3) row.add(ItemDto.placeholder());
            rows.add(row);
        }
        return new ItemPage(rows, query, validSort,
                new PagingDto(size, page, result.hasPrevious(), result.hasNext()));
    }

    @Transactional(readOnly = true)
    public ItemDto get(long id) {
        var item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден"));
        return ItemDto.from(item, cartService.quantities().getOrDefault(id, 0));
    }
}
