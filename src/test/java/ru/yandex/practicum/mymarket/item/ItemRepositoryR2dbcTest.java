package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.*;

@DataR2dbcTest
@TestPropertySource(properties = "spring.r2dbc.url=r2dbc:h2:mem:///repositorytest;DB_CLOSE_DELAY=-1")
class ItemRepositoryR2dbcTest {
    @Autowired ItemRepository repository;

    @BeforeEach void clear() { repository.deleteAll().block(); }

    @Test void savesAndFindsReactively() {
        StepVerifier.create(repository.save(new ItemEntity("Мяч", "Для спорта", "images/ball.svg", 500))
                .flatMap(item -> repository.findById(item.getId())))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Мяч"))
                .verifyComplete();
    }

    @Test void filtersAndSorts() {
        repository.save(new ItemEntity("Яблоко", "Съедобное", "images/ball.svg", 300)).block();
        repository.save(new ItemEntity("Арбуз", "Съедобный", "images/ball.svg", 100)).block();
        StepVerifier.create(repository.searchPrice("", 5, 0))
                .assertNext(item -> assertThat(item.getPrice()).isEqualTo(100))
                .assertNext(item -> assertThat(item.getPrice()).isEqualTo(300))
                .verifyComplete();
    }
}
