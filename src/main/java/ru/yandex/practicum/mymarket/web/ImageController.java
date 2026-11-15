package ru.yandex.practicum.mymarket.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ImageController {
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        if (!filename.matches("[a-zA-Z0-9_-]+\\.(svg|png|jpg|jpeg)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Resource image = new ClassPathResource("static/images/" + filename);
        if (!image.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        MediaType type = filename.endsWith(".svg") ? MediaType.parseMediaType("image/svg+xml")
                : filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(image);
    }
}
