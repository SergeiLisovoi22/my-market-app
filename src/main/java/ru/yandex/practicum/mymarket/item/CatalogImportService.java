package ru.yandex.practicum.mymarket.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.mymarket.common.Money;

@Service
public class CatalogImportService {
    private final ItemRepository repository;

    public CatalogImportService(ItemRepository repository) { this.repository = repository; }

    // Учебный CSV: title;description;price;imgPath, без кавычек и ';' внутри полей.
    @Transactional
    public int importCsv(MultipartFile file) throws IOException {
        List<ItemEntity> items = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
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
                if (fields.length != 4) {
                    throw new IllegalArgumentException("В строке " + lineNumber + " ожидается 4 поля через ';'");
                }
                String title = fields[0].trim();
                String description = fields[1].trim();
                String priceText = fields[2].trim();
                String imagePath = fields[3].trim();
                if (title.isBlank()) throw new IllegalArgumentException("В строке " + lineNumber + " не указано название товара");
                if (title.length() > ItemEntity.TITLE_MAX) {
                    throw new IllegalArgumentException("В строке " + lineNumber + " название товара слишком длинное (максимум " + ItemEntity.TITLE_MAX + ")");
                }
                if (description.isBlank()) throw new IllegalArgumentException("В строке " + lineNumber + " не указано описание товара");
                if (description.length() > ItemEntity.DESCRIPTION_MAX) {
                    throw new IllegalArgumentException("В строке " + lineNumber + " описание слишком длинное");
                }
                if (imagePath.isBlank()) throw new IllegalArgumentException("В строке " + lineNumber + " не указан путь к изображению");
                if (imagePath.length() > ItemEntity.IMAGE_PATH_MAX) {
                    throw new IllegalArgumentException("В строке " + lineNumber + " путь к изображению слишком длинный");
                }
                long price;
                try {
                    price = Long.parseLong(priceText);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("В строке " + lineNumber + " неверная цена", ex);
                }
                try {
                    Money.validatePrice(price);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("В строке " + lineNumber + ": " + ex.getMessage(), ex);
                }
                items.add(new ItemEntity(title, description, imagePath, price));
            }
        }
        // До этой строки всё содержимое файла проверено. Ошибочная строка не создаст частичный импорт.
        repository.saveAll(items);
        return items.size();
    }
}
