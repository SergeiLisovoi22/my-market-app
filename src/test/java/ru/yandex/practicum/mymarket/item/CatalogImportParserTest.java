package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CatalogImportParserTest {
    @Test void acceptsHeaderAfterBlankLine() throws Exception {
        var result = CatalogImportService.parse("\ntitle;description;price;imgPath\nТовар;Описание;123;images/ball.svg\n");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Товар");
    }
    @Test void rejectsTooLongTitleWithLineNumber() {
        var csv = "title;description;price;imgPath\nНормальный;Описание;100;images/ball.svg\n"
                + "X".repeat(256) + ";Описание;100;images/ball.svg\n";
        assertThatThrownBy(() -> CatalogImportService.parse(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("строке 3");
    }
    @Test void rejectsInvalidPrice() {
        assertThatThrownBy(() -> CatalogImportService.parse("Товар;Описание;1000000001;images/ball.svg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("строке 1");
    }
    @Test void rejectsMalformedRow() {
        assertThatThrownBy(() -> CatalogImportService.parse("Товар;Описание"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
