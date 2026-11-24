package ru.yandex.practicum.mymarket;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartLineRepository;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.item.CatalogImportService;
import ru.yandex.practicum.mymarket.item.ItemEntity;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.order.OrderLineRepository;
import ru.yandex.practicum.mymarket.order.OrderRepository;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.order.EmptyCartException;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ShopFlowReactiveIntegrationTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    @Autowired WebTestClient client;
    @Autowired CartService cart;
    @Autowired OrderService orderService;
    @Autowired ItemRepository itemRepository;
    @Autowired CatalogImportService importer;
    @Autowired CartLineRepository cartRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderLineRepository orderLineRepository;
    private ItemEntity ball;
    private ItemEntity cap;

    @BeforeEach void prepareCatalog() {
        client = client.mutate().responseTimeout(TIMEOUT)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build();
        orderLineRepository.deleteAll().then(orderRepository.deleteAll())
                .then(cartRepository.deleteAll()).then(itemRepository.deleteAll()).block(TIMEOUT);
        ball = itemRepository.save(new ItemEntity("Тестовый мяч", "Для футбола", "images/ball.svg", 1800)).block(TIMEOUT);
        cap = itemRepository.save(new ItemEntity("Тестовая кепка", "Для прогулок", "images/cap-red.svg", 1200)).block(TIMEOUT);
        itemRepository.save(new ItemEntity("Тестовый зонт", "Для дождя", "images/umbrella.svg", 900)).block(TIMEOUT);
    }

    @Test void rendersCatalogAndDetail() {
        Document catalog = html(client.get().uri("/items"));
        assertThat(titles(catalog)).containsExactly(ball.getTitle(), cap.getTitle(), "Тестовый зонт");
        Document detail = html(client.get().uri("/items/" + ball.getId()));
        assertProductCard(detail.selectFirst(".card"), ball, 0);
        assertThat(detail.selectFirst(".card-text").text()).isEqualTo("Для футбола");
        client.get().uri("/images/ball.svg").exchange().expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("image/svg+xml"));
    }

    @Test void supportsSearchAndSorting() {
        assertThat(titles(html(client.get().uri(b -> b.path("/items").queryParam("search", "футбола").build()))))
                .containsExactly(ball.getTitle());
        assertThat(titles(html(client.get().uri("/items?sort=PRICE"))))
                .containsExactly("Тестовый зонт", cap.getTitle(), ball.getTitle());
        assertThat(titles(html(client.get().uri("/items?sort=ALPHA"))))
                .containsExactly("Тестовая кепка", "Тестовый зонт", "Тестовый мяч");
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 10, 20, 50, 100})
    void supportsPagination(int size) {
        List<String> expected = IntStream.rangeClosed(1, 123)
                .mapToObj(i -> "Page item %03d".formatted(i)).toList();
        itemRepository.saveAll(expected.stream()
                .map(title -> new ItemEntity(title, "Pagination fixture", "images/ball.svg", 100)).toList())
                .then().block(TIMEOUT);
        Document first = page(size, 1);
        Document second = page(size, 2);
        int lastPage = (expected.size() + size - 1) / size;
        Document last = page(size, lastPage);
        assertThat(titles(first)).containsExactlyElementsOf(expected.subList(0, size));
        assertThat(titles(second)).containsExactlyElementsOf(expected.subList(size, Math.min(2 * size, expected.size())));
        assertThat(titles(first)).doesNotContainAnyElementsOf(titles(second));
        assertThat(titles(last)).containsExactlyElementsOf(expected.subList((lastPage - 1) * size, expected.size()));
        assertThat(titles(last)).hasSize(expected.size() % size);
        assertThat(first.select("button[name=pageNumber][value=2]")).hasSize(1);
        assertThat(first.select("button[name=pageNumber][value=0]")).isEmpty();
        assertThat(second.select("button[name=pageNumber][value=1]")).hasSize(1);
        assertThat(last.select("button[name=pageNumber][value=" + (lastPage + 1) + "]")).isEmpty();
        assertThat(last.select("button[name=pageNumber][value=" + (lastPage - 1) + "]")).hasSize(1);
    }

    private Document page(int size, int number) {
        return html(client.get().uri(b -> b.path("/items").queryParam("search", "Page item")
                .queryParam("sort", "ALPHA").queryParam("pageSize", size).queryParam("pageNumber", number).build()));
    }

    @Test void rejectsHugePageNumber() {
        client.get().uri("/items?pageNumber=100000000&pageSize=100").exchange()
                .expectStatus().isBadRequest().expectBody(String.class)
                .value(body -> assertThat(body).contains("слишком большой номер страницы"));
    }

    @Test void allowsEmptyValidPage() {
        Document empty = html(client.get().uri("/items?pageNumber=1000&pageSize=100"));
        assertThat(titles(empty)).isEmpty();
        assertThat(empty.selectFirst(".alert-info a[href='/items']").text())
                .isEqualTo("Вернуться на первую страницу");
    }

    @ParameterizedTest
    @CsvSource({"'two words',two%20words", "C++,C%2B%2B", "A&B,A%26B", "50%,50%25", "C#,C%23"})
    void formPostStrictlyEncodesSearch(String search, String encoded) {
        URI target = redirect(client.post().uri("/items").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", ball.getId().toString()).with("action", "PLUS")
                        .with("search", search).with("sort", "PRICE").with("pageSize", "10").with("pageNumber", "2")));
        assertThat(target.getPath()).isEqualTo("/items");
        assertThat(target.getRawQuery()).contains("search=" + encoded + "&", "sort=PRICE", "pageSize=10", "pageNumber=2");
        assertThat(target.getFragment()).isNull();
        String actual = URLDecoder.decode(target.getRawQuery().split("&")[0].substring("search=".length()), StandardCharsets.UTF_8);
        assertThat(actual).isEqualTo(search);
        // Проверяем не только Location, но и значение поиска после реального GET.
        Document after = follow(target);
        assertThat(after.selectFirst("input[type=search]").val()).isEqualTo(search);
        assertCart(1800, 1, 0);
    }

    @Test void minusAndDeleteUpdateCartContentsAndTotal() {
        changeCart(ball, "PLUS");
        changeCart(ball, "PLUS");
        changeCart(cap, "PLUS");
        changeCart(cap, "PLUS");
        assertCart(6000, 2, 2);
        changeCart(ball, "MINUS");
        assertCart(4200, 1, 2);
        changeCart(ball, "MINUS");
        assertCart(2400, 0, 2);
        changeCart(cap, "DELETE");
        assertCart(0, 0, 0);
    }

    @Test void checkoutThroughHttpPersistsExactOrderAndClearsCart() {
        // Добавление из витрины и карточки товара, затем покупка двух разных товаров.
        URI fromList = redirect(client.post().uri("/items").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", ball.getId().toString()).with("action", "PLUS")));
        assertThat(fromList.getPath()).isEqualTo("/items");
        URI fromDetail = redirect(client.post().uri("/items/" + ball.getId()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("action", "PLUS")));
        assertThat(fromDetail.getPath()).isEqualTo("/items/" + ball.getId());
        assertProductCard(follow(fromDetail).selectFirst(".card"), ball, 2);
        changeCart(cap, "PLUS");
        assertCart(4800, 2, 1);
        URI firstLocation = redirect(client.post().uri("/buy"));
        assertThat(firstLocation.getPath()).matches("/orders/[0-9]+");
        assertThat(firstLocation.getQuery()).isEqualTo("newOrder=true");
        long firstId = orderId(firstLocation);
        Document order = follow(firstLocation);
        assertThat(order.text()).contains("Успешная покупка", "Заказ №" + firstId);
        assertThat(order.select(".card-header b").eachText()).containsExactly(ball.getTitle(), cap.getTitle());
        assertOrderPosition(order.select(".card").get(0), ball, 2, 3600);
        assertOrderPosition(order.select(".card").get(1), cap, 1, 1200);
        assertThat(order.selectFirst("h3").text()).isEqualTo("Сумма: 4800 руб.");
        assertCart(0, 0, 0);
        StepVerifier.create(orderService.get(firstId)).assertNext(saved -> {
            assertThat(saved.totalSum()).isEqualTo(4800);
            assertThat(saved.items()).hasSize(2);
            assertThat(saved.items().get(0).price()).isEqualTo(1800);
            assertThat(saved.items().get(0).count()).isEqualTo(2);
            assertThat(saved.items().get(0).imgPath()).isEqualTo(ball.getImgPath());
            assertThat(saved.items().get(1).id()).isEqualTo(cap.getId());
        }).expectComplete().verify(TIMEOUT);
        // Второй заказ делает проверку общей суммы нетривиальной и проверяет группировку позиций.
        changeCart(cap, "PLUS");
        URI secondLocation = redirect(client.post().uri("/buy"));
        long secondId = orderId(secondLocation);
        assertCart(0, 0, 0);
        Document history = html(client.get().uri("/orders"));
        assertThat(history.select(".card-header a").eachText()).containsExactly("Заказ №" + secondId, "Заказ №" + firstId);
        assertThat(history.select(".card-header a").eachAttr("href")).containsExactly("/orders/" + secondId, "/orders/" + firstId);
        assertThat(history.select(".card-footer b").eachText()).containsExactly("Сумма: 1200 руб.", "Сумма: 4800 руб.");
        assertThat(history.selectFirst("h3").text()).isEqualTo("Итого по всем заказам: 6000 руб.");
        var historyCards = history.select(".card");
        assertHistoryPosition(historyCards.get(0).select(".order-position").get(0), cap, 1, 1200);
        assertThat(historyCards.get(0).select(".order-position")).hasSize(1);
        assertThat(historyCards.get(1).select(".order-position")).hasSize(2);
        assertHistoryPosition(historyCards.get(1).select(".order-position").get(0), ball, 2, 3600);
        assertHistoryPosition(historyCards.get(1).select(".order-position").get(1), cap, 1, 1200);
        Document reopened = html(client.get().uri(historyCards.get(1).selectFirst(".card-header a").attr("href")));
        assertThat(reopened.selectFirst("h3").text()).isEqualTo("Сумма: 4800 руб.");
        assertThat(reopened.text()).doesNotContain("Успешная покупка");
    }

    @Test void emptyCheckoutFailsWithoutCreatingOrder() {
        StepVerifier.create(orderService.checkout()).expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(EmptyCartException.class).hasMessage("Корзина пуста");
        }).verify(TIMEOUT);
        client.post().uri("/buy").exchange().expectStatus().isBadRequest()
                .expectBody(String.class).value(body -> assertThat(body).contains("Корзина пуста"));
        assertThat(orderRepository.count().block(TIMEOUT)).isZero();
        assertThat(orderLineRepository.count().block(TIMEOUT)).isZero();
        assertThat(html(client.get().uri("/orders")).selectFirst("h3").text()).isEqualTo("Итого по всем заказам: 0 руб.");
    }

    @Test void csvImportIsAtomicOnValidationError() {
        String bad = "title;description;price;imgPath\n"
                + "Новый товар;Описание;120;images/ball.svg\n"
                + "X".repeat(256) + ";Описание;100;images/ball.svg\n";
        long before = itemRepository.count().block(TIMEOUT);
        StepVerifier.create(importer.importText(bad)).expectErrorMatches(error ->
                error instanceof IllegalArgumentException && error.getMessage().contains("строке 3"))
                .verify(TIMEOUT);
        assertThat(itemRepository.count().block(TIMEOUT)).isEqualTo(before);
    }

    @Test void multipartImportPersistsProductAndShowsItOnCatalog() {
        long before = itemRepository.count().block(TIMEOUT);
        URI target = redirect(upload("title;description;price;imgPath\nИмпортная кружка;Стальная;790;images/bottle.svg\n"
                .getBytes(StandardCharsets.UTF_8)));
        assertThat(target.getPath()).isEqualTo("/admin/items/import");
        assertThat(follow(target).selectFirst(".alert-success").text()).isEqualTo("Импортировано товаров: 1");
        StepVerifier.create(itemRepository.searchDefault("Импортная кружка", 5, 0)).assertNext(item -> {
            assertThat(item.getTitle()).isEqualTo("Импортная кружка");
            assertThat(item.getDescription()).isEqualTo("Стальная");
            assertThat(item.getPrice()).isEqualTo(790);
            assertThat(item.getImgPath()).isEqualTo("images/bottle.svg");
        }).expectComplete().verify(TIMEOUT);
        assertThat(itemRepository.count().block(TIMEOUT)).isEqualTo(before + 1);
        Document catalog = html(client.get().uri(b -> b.path("/items").queryParam("search", "Импортная кружка").build()));
        assertThat(titles(catalog)).containsExactly("Импортная кружка");
        assertThat(catalog.selectFirst(".card .badge").text()).isEqualTo("790 руб.");
        assertThat(catalog.selectFirst(".card img").attr("src")).isEqualTo("/images/bottle.svg");
    }

    @Test void invalidMultipartShowsValidationErrorWithoutPartialImport() {
        long before = itemRepository.count().block(TIMEOUT);
        String bad = "title;description;price;imgPath\n"
                + "Valid product;Description;120;images/ball.svg\n"
                + "Broken row;Description\n";
        URI target = redirect(upload(bad.getBytes(StandardCharsets.UTF_8)));
        assertThat(follow(target).selectFirst(".alert-danger").text()).contains("строке 3", "4 поля");
        assertThat(itemRepository.count().block(TIMEOUT)).isEqualTo(before);
        StepVerifier.create(itemRepository.searchDefault("Valid product", 5, 0))
                .expectComplete().verify(TIMEOUT);
    }

    @Test void rejectsOversizedMultipartWithoutChangingCatalog() {
        long before = itemRepository.count().block(TIMEOUT);
        // Лимит test-профиля: 4096 байт, меньше общего лимита multipart.
        byte[] tooLarge = ("x".repeat(importer.maxBytes() + 1)).getBytes(StandardCharsets.UTF_8);
        upload(tooLarge).exchange().expectStatus().isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE)
                .expectBody(String.class).value(body -> assertThat(body).contains("CSV-файл слишком большой", "4096"));
        assertThat(itemRepository.count().block(TIMEOUT)).isEqualTo(before);
        assertThat(html(client.get().uri("/admin/items/import")).text()).contains("Максимальный размер файла: 4096 байт");
    }

    private WebTestClient.RequestHeadersSpec<?> upload(byte[] bytes) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.part("file", new ByteArrayResource(bytes)).filename("catalog.csv").contentType(MediaType.valueOf("text/csv"));
        return client.post().uri("/admin/items/import").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts.build()));
    }

    private void changeCart(ItemEntity item, String action) {
        URI result = redirect(client.post().uri("/cart/items").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", item.getId().toString()).with("action", action)));
        assertThat(result.getPath()).isEqualTo("/cart/items");
    }

    private void assertCart(long total, int ballCount, int capCount) {
        StepVerifier.create(cart.getCart()).assertNext(dto -> {
            assertThat(dto.total()).isEqualTo(total);
            assertThat(dto.items()).extracting(ItemDto::id).containsExactlyElementsOf(expectedIds(ballCount, capCount));
            assertThat(dto.items()).allSatisfy(item -> {
                int expected = item.id() == ball.getId() ? ballCount : capCount;
                assertThat(item.count()).isEqualTo(expected);
            });
        }).expectComplete().verify(TIMEOUT);
        Document document = html(client.get().uri("/cart/items"));
        assertThat(document.select(".card")).hasSize(expectedIds(ballCount, capCount).size());
        int i = 0;
        if (ballCount > 0) assertProductCard(document.select(".card").get(i++), ball, ballCount);
        if (capCount > 0) assertProductCard(document.select(".card").get(i), cap, capCount);
        if (ballCount + capCount == 0) assertThat(document.select("form[action='/buy']")).isEmpty();
        else assertThat(document.select("form[action='/buy'] h2").eachText())
                .containsExactly("Итого: " + total + " руб.");
    }

    private List<Long> expectedIds(int ballCount, int capCount) {
        return java.util.stream.Stream.of(ballCount > 0 ? ball.getId() : null, capCount > 0 ? cap.getId() : null)
                .filter(java.util.Objects::nonNull).toList();
    }

    private static void assertProductCard(Element card, ItemEntity item, int count) {
        assertThat(card).isNotNull();
        assertThat(card.selectFirst(".card-title").text()).isEqualTo(item.getTitle());
        assertThat(card.selectFirst(".badge").text()).isEqualTo(item.getPrice() + " руб.");
        assertThat(card.selectFirst("img").attr("src")).isEqualTo("/" + item.getImgPath());
        assertThat(card.selectFirst(".item-count").text()).isEqualTo(String.valueOf(count));
    }

    private static void assertOrderPosition(Element card, ItemEntity item, int count, long sum) {
        assertThat(card.selectFirst(".card-header b").text()).isEqualTo(item.getTitle());
        assertThat(card.selectFirst(".position-price").text()).isEqualTo(item.getPrice() + " руб.");
        assertThat(card.selectFirst(".position-count").text()).isEqualTo(count + " шт.");
        assertThat(card.selectFirst("img").attr("src")).isEqualTo("/" + item.getImgPath());
        assertThat(card.selectFirst(".card-footer b").text()).isEqualTo("Сумма: " + sum + " руб.");
    }

    private static void assertHistoryPosition(Element position, ItemEntity item, int count, long sum) {
        assertThat(position.selectFirst(".position-title").text()).isEqualTo(item.getTitle());
        assertThat(position.selectFirst(".position-price").text()).isEqualTo(item.getPrice() + " руб.");
        assertThat(position.selectFirst(".position-count").text()).isEqualTo(count + " шт.");
        assertThat(position.selectFirst("img").attr("src")).isEqualTo("/" + item.getImgPath());
        assertThat(position.selectFirst(".position-total").text()).isEqualTo("Сумма: " + sum + " руб.");
    }

    private static List<String> titles(Document document) { return document.select(".catalog-grid .card-title").eachText(); }

    private static Document html(WebTestClient.RequestHeadersSpec<?> request) {
        String body = request.exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).isNotNull();
        return Jsoup.parse(body);
    }

    private static URI redirect(WebTestClient.RequestHeadersSpec<?> request) {
        URI location = request.exchange().expectStatus().is3xxRedirection().expectBody().returnResult()
                .getResponseHeaders().getLocation();
        assertThat(location).isNotNull();
        return location;
    }

    private Document follow(URI location) {
        return html(client.get().uri(builder -> builder.build().resolve(location)));
    }

    private static long orderId(URI location) {
        return Long.parseLong(location.getPath().substring("/orders/".length()));
    }
}
