package ru.yandex.practicum.mymarket.config;

import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import ru.yandex.practicum.mymarket.item.ItemRepository;

@Configuration
public class SampleDataConfig {
    @Bean
    ApplicationRunner sampleItems(ItemRepository repository) {
        return args -> {
            // Единственное блокирующее ожидание при начальной загрузке, вне HTTP-обработчиков.
            repository.count().filter(count -> count == 0)
                    .flatMapMany(ignored -> repository.saveAll(List.of(
                            new ItemEntity("Бейсболка чёрная", "Удобная бейсболка для прогулок", "images/cap-black.svg", 1200),
                            new ItemEntity("Футбольный мяч", "Классический футбольный мяч", "images/ball.svg", 1800),
                            new ItemEntity("Бейсболка красная", "Яркая кепка на каждый день", "images/cap-red.svg", 1500),
                            new ItemEntity("Зонт", "Компактный зонт для дождливой погоды", "images/umbrella.svg", 900),
                            new ItemEntity("Спортивная бутылка", "Многоразовая бутылка для воды", "images/bottle.svg", 650),
                            new ItemEntity("Рюкзак", "Лёгкий городской рюкзак", "images/backpack.svg", 3200)
                    )))
                    .then().block();
        };
    }
}
