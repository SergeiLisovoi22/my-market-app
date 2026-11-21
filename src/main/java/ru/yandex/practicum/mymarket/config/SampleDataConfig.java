package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import java.util.List;
import ru.yandex.practicum.mymarket.cart.CartMutex;
import ru.yandex.practicum.mymarket.cart.CartMutexRepository;

@Configuration
public class SampleDataConfig {
    @Bean
    ApplicationRunner sampleItems(ItemRepository repository, CartMutexRepository mutexRepository) {
        return args -> {
            if (!mutexRepository.existsById(1L)) {
                mutexRepository.saveAndFlush(new CartMutex(1L));
            }
            if (repository.count() != 0) return;
            repository.saveAll(List.of(
                new ItemEntity("Бейсболка чёрная", "Удобная бейсболка для прогулок", "images/cap-black.svg", 1200),
                new ItemEntity("Футбольный мяч", "Классический футбольный мяч", "images/ball.svg", 1800),
                new ItemEntity("Бейсболка красная", "Яркая кепка на каждый день", "images/cap-red.svg", 1500),
                new ItemEntity("Зонт", "Компактный зонт для дождливой погоды", "images/umbrella.svg", 900),
                new ItemEntity("Спортивная бутылка", "Многоразовая бутылка для воды", "images/bottle.svg", 650),
                new ItemEntity("Рюкзак", "Лёгкий городской рюкзак", "images/backpack.svg", 3200)
            ));
        };
    }
}
