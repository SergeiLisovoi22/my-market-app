package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ItemRepositoryTest {
    @Autowired ItemRepository repository;

    @Test
    void searchFindsItemsCaseInsensitivelyInTitleOrDescription() {
        repository.save(new ItemEntity("Футбольный мяч", "Для игры", "images/ball.svg", 200));
        repository.save(new ItemEntity("Кепка", "Для футбола", "images/cap-red.svg", 400));
        var result = repository.search("ФУТБ", PageRequest.of(0, 5, Sort.by("id")));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void supportsPaginationAndPriceSorting() {
        repository.save(new ItemEntity("A", "", "images/ball.svg", 300));
        repository.save(new ItemEntity("B", "", "images/ball.svg", 100));
        repository.save(new ItemEntity("C", "", "images/ball.svg", 200));
        var page = repository.search("", PageRequest.of(0, 2, Sort.by("price")));
        assertThat(page.getContent()).extracting(ItemEntity::getPrice).containsExactly(100L, 200L);
        assertThat(page.hasNext()).isTrue();
    }
}
