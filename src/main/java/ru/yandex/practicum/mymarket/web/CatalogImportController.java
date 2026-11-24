package ru.yandex.practicum.mymarket.web;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.item.CatalogImportService;

@Controller
@RequestMapping("/admin/items")
public class CatalogImportController {
    private final CatalogImportService importer;
    public CatalogImportController(CatalogImportService importer) { this.importer = importer; }

    @GetMapping("/import")
    public String form(@RequestParam(required = false) String message,
                       @RequestParam(required = false) String error, Model model) {
        model.addAttribute("maxImportBytes", importer.maxBytes());
        if (message != null) model.addAttribute("message", message);
        if (error != null) model.addAttribute("error", error);
        return "import-items";
    }

    @PostMapping("/import")
    public Mono<String> importItems(@RequestPart("file") FilePart file) {
        if (file.filename().isBlank()) return Mono.just(redirect("error", "Выберите CSV-файл"));
        return importer.importCsv(file)
                .map(count -> redirect("message", "Импортировано товаров: " + count))
                .onErrorMap(DataBufferLimitException.class, ex -> new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "CSV-файл слишком большой. Максимальный размер: " + importer.maxBytes() + " байт", ex))
                .onErrorResume(IllegalArgumentException.class, ex -> Mono.just(redirect("error", ex.getMessage())));
    }

    private static String redirect(String key, String message) {
        return "redirect:" + UriComponentsBuilder.fromPath("/admin/items/import")
                .queryParam(key, "{message}").encode().buildAndExpand(message).toUriString();
    }
}
