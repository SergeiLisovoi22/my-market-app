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

@Service
public class CatalogImportService {
    private final ItemRepository repository;
    public CatalogImportService(ItemRepository repository) { this.repository = repository; }

    // Простой учебный формат CSV: title;description;price;imgPath (без кавычек и ';' внутри полей).
    @Transactional
    public int importCsv(MultipartFile file) throws IOException {
        List<ItemEntity> items = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                if (lineNumber == 1) line = line.replace("\uFEFF", "");
                if (lineNumber == 1 && line.startsWith("title;")) continue;
                String[] parts = line.split(";", -1);
                if (parts.length != 4) {
                    throw new IllegalArgumentException("Строка " + lineNumber + ": ожидается 4 поля через ';'");
                }
                try {
                    items.add(new ItemEntity(parts[0].trim(), parts[1].trim(),
                            parts[3].trim(), Long.parseLong(parts[2].trim())));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Строка " + lineNumber + ": неверная цена", ex);
                }
            }
        }
        repository.saveAll(items);
        return items.size();
    }
}
