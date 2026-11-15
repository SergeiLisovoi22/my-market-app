package ru.yandex.practicum.mymarket.web;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
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
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите CSV-файл");
        try {
            int count = importer.importCsv(file);
            redirect.addFlashAttribute("message", "Импортировано товаров: " + count);
            return "redirect:/admin/items/import";
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
