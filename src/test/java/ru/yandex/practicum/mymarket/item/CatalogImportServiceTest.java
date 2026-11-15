package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogImportServiceTest {
    private final ItemRepository repository = mock(ItemRepository.class);
    private final CatalogImportService service = new CatalogImportService(repository);

    @Test
    void importsTwoValidItems() throws Exception {
        var csv = file("title;description;price;imgPath\n"
                + "Термокружка;Для чая;790;images/bottle.svg\n"
                + "Сумка;Для города;2400;images/backpack.svg\n");

        assertThat(service.importCsv(csv)).isEqualTo(2);
        verify(repository).saveAll(argThat(items ->
                StreamSupport.stream(items.spliterator(), false).count() == 2));
    }

    @Test
    void malformedRowIsRejectedBeforeSavingAnything() {
        var csv = file("title;description;price;imgPath\n"
                + "Хороший товар;Описание;100;images/ball.svg\n"
                + "Некорректная строка;без цены\n");

        assertThatThrownBy(() -> service.importCsv(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Строка 3");
        verify(repository, never()).saveAll(any());
    }

    @Test
    void negativePriceIsRejected() {
        var csv = file("Товар;Описание;-1;images/ball.svg\n");

        assertThatThrownBy(() -> service.importCsv(csv))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAll(any());
    }

    private static MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "catalog.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
