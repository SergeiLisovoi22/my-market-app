package ru.yandex.practicum.mymarket.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.item.CatalogImportService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogImportController.class)
class CatalogImportControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CatalogImportService importer;

    @Test
    void validationErrorRedirectsBackToFormWithMessage() throws Exception {
        var file = new MockMultipartFile("file", "catalog.csv", "text/csv", "bad csv".getBytes());
        when(importer.importCsv(any())).thenThrow(new IllegalArgumentException("В строке 3 название товара слишком длинное"));

        mvc.perform(multipart("/admin/items/import").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/items/import"))
                .andExpect(flash().attribute("error", "В строке 3 название товара слишком длинное"));
    }
}
