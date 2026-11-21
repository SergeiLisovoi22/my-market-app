package ru.yandex.practicum.mymarket.web;

import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.item.CatalogImportService;

@Controller
@RequestMapping("/admin/items")
public class CatalogImportController {
    private final CatalogImportService importer;
    public CatalogImportController(CatalogImportService importer) { this.importer = importer; }

    @GetMapping("/import")
    public String form() { return "import-items"; }

    @PostMapping("/import")
    public String importItems(@RequestParam("file") MultipartFile file,
                              RedirectAttributes redirect) {
        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Выберите CSV-файл");
            return "redirect:/admin/items/import";
        }
        try {
            int count = importer.importCsv(file);
            redirect.addFlashAttribute("message", "Импортировано товаров: " + count);
        } catch (IOException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/items/import";
    }
}
