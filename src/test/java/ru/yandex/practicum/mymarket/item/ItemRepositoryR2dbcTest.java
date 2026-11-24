package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.*;

@DataR2dbcTest
@TestPropertySource(properties = "spring.r2dbc.url=r2dbc:h2:mem:///repositorytest;DB_CLOSE_DELAY=-1")
class ItemRepositoryR2dbcTest {
    @Autowired ItemRepository repository;
    @Autowired DatabaseClient database;

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

    @Test void databaseRejectsInvalidPrice() {
        StepVerifier.create(database.sql("""
                INSERT INTO items (title, description, img_path, price)
                VALUES ('invalid', 'desc', 'images/ball.svg', -1)
                """).fetch().rowsUpdated())
                .expectError(DataIntegrityViolationException.class).verify();
    }

    @Test void databaseRejectsInvalidCartQuantity() {
        var product = repository.save(new ItemEntity("Мяч", "Описание", "images/ball.svg", 100)).block();
        StepVerifier.create(database.sql("INSERT INTO cart_lines (item_id, count) VALUES (:id, 0)")
                .bind("id", product.getId()).fetch().rowsUpdated())
                .expectError(DataIntegrityViolationException.class).verify();
    }

    @Test void databaseRejectsNegativeOrderTotal() {
        StepVerifier.create(database.sql("""
                INSERT INTO purchase_orders (total_sum, created_at) VALUES (-1, CURRENT_TIMESTAMP)
                """).fetch().rowsUpdated())
                .expectError(DataIntegrityViolationException.class).verify();
    }
}
