package ru.yandex.practicum.mymarket.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.yandex.practicum.mymarket.common.Money;

@Service
public class CatalogImportService {
    private final ItemRepository repository;
    private final TransactionalOperator tx;

    public CatalogImportService(ItemRepository repository, TransactionalOperator tx) {
        this.repository = repository; this.tx = tx;
    }

    public Mono<Integer> importCsv(FilePart file) {
        return DataBufferUtils.join(file.content(), 5 * 1024 * 1024)
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Файл пуст")))
                .flatMap(this::importText);
    }

    /** Проверяем файл целиком до начала записи, затем сохраняем атомарно. */
    public Mono<Integer> importText(String csv) {
        return Mono.fromCallable(() -> parse(csv))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(items -> repository.saveAll(items).then(Mono.just(items.size()))
                        .as(tx::transactional));
    }

    static List<ItemEntity> parse(String csv) throws IOException {
        List<ItemEntity> items = new ArrayList<>();
        try (var reader = new BufferedReader(new StringReader(csv))) {
            String line;
            int lineNumber = 0;
            boolean firstNonBlank = true;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                if (firstNonBlank) {
                    firstNonBlank = false;
                    line = line.replace("\uFEFF", "").trim();
                    if (line.equalsIgnoreCase("title;description;price;imgPath")) continue;
                }
                String[] fields = line.split(";", -1);
                if (fields.length != 4)
                    throw new IllegalArgumentException("В строке " + lineNumber + " ожидается 4 поля через ';'");
                String title = fields[0].trim();
                String description = fields[1].trim();
                String priceText = fields[2].trim();
                String imagePath = fields[3].trim();
                if (title.isBlank()) throw new IllegalArgumentException("В строке " + lineNumber + " не указано название товара");
                if (title.length() > ItemEntity.TITLE_MAX)
                    throw new IllegalArgumentException("В строке " + lineNumber + " название товара слишком длинное");
                if (description.isBlank()) throw new IllegalArgumentException("В строке " + lineNumber + " не указано описание товара");
                if (description.length() > ItemEntity.DESCRIPTION_MAX)
                    throw new IllegalArgumentException("В строке " + lineNumber + " описание слишком длинное");
                if (imagePath.isBlank()) throw new IllegalArgumentException("В строке " + lineNumber + " не указан путь к изображению");
                if (imagePath.length() > ItemEntity.IMAGE_PATH_MAX)
                    throw new IllegalArgumentException("В строке " + lineNumber + " путь к изображению слишком длинный");
                long price;
                try { price = Long.parseLong(priceText); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("В строке " + lineNumber + " неверная цена", ex); }
                try { Money.validatePrice(price); }
                catch (IllegalArgumentException ex) { throw new IllegalArgumentException("В строке " + lineNumber + ": " + ex.getMessage(), ex); }
                items.add(new ItemEntity(title, description, imagePath, price));
            }
        }
        return items;
    }
}
