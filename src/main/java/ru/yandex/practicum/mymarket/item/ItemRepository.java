package ru.yandex.practicum.mymarket.item;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends ReactiveCrudRepository<ItemEntity, Long> {
    @Query("""
        SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY id LIMIT :limit OFFSET :offset
        """)
    Flux<ItemEntity> searchDefault(@Param("search") String search, @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY title ASC, id ASC LIMIT :limit OFFSET :offset
        """)
    Flux<ItemEntity> searchAlpha(@Param("search") String search, @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY price ASC, id ASC LIMIT :limit OFFSET :offset
        """)
    Flux<ItemEntity> searchPrice(@Param("search") String search, @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        SELECT COUNT(*) FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Mono<Long> countMatching(@Param("search") String search);
}
