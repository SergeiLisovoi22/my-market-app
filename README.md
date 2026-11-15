# My Market App — Sprint 5 (Java 21 / Spring Boot)

Учебный проект «Витрина интернет-магазина» по заданию Яндекс Практикума.
Макет страниц сохранён; добавлены backend, H2, JPA, тесты и Docker.

## Требования

- Java 21 (например, Amazon Corretto 21)
- Gradle Wrapper 8.8 входит в проект, отдельная установка Gradle не требуется
- Для Docker-запуска: Docker Desktop

## Локальный запуск

1. Открыть директорию `my-market-app` в IntelliJ IDEA как **Gradle**-проект.
2. В настройках Gradle выбрать JDK 21.
3. В терминале IntelliJ IDEA выполнить `.\gradlew.bat bootRun` (Windows PowerShell) или `./gradlew bootRun` (Linux/macOS).
4. Открыть <http://localhost:8080/items>.

В архиве уже присутствуют `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` и `gradle-wrapper.properties`: добавьте их в Git. При первом запуске Wrapper скачает Gradle 8.8, если дистрибутив ещё не закеширован.

При первом запуске создаются 6 демонстрационных товаров. База H2 файловая:
`./data/marketdb` (состояние сохраняется между запусками).

## Тесты и JAR

```bash
./gradlew clean test
./gradlew bootJar
java -jar build/libs/my-market-app-0.0.1-SNAPSHOT.jar
```

На Windows PowerShell используйте `.\gradlew.bat` вместо `./gradlew`.
Отчёт о тестах: `build/reports/tests/test/index.html`.

## Docker

```bash
./gradlew bootJar
docker compose up --build
```

Приложение доступно по адресу <http://localhost:8080/items>.
База данных сохраняется в именованном Docker-томе `market-data`.

## Страницы и действия

- `GET /`, `GET /items` — витрина, поиск, сортировка, пагинация.
- `POST /items` — изменение количества товара (PLUS/MINUS), редирект на витрину.
- `GET /items/{id}`, `POST /items/{id}` — карточка товара и действия с корзиной.
- `GET /cart`, `GET /cart/items`, `POST /cart/items` — корзина (PLUS/MINUS/DELETE).
- `GET /orders`, `GET /orders/{id}` — список заказов и детали.
- `POST /buy` — сохранение заказа и очистка корзины в одной транзакции.
- `GET /images/{filename}` — локальные изображения товаров.
- `GET /admin/items/import`, `POST /admin/items/import` — простой импорт товаров из CSV.

CSV: `title;description;price;imgPath`. Образец: `samples/catalog.csv`.
Для файлов изображений в данном MVP используются локальные SVG.

## Архитектура

`web (MVC) → service (бизнес-логика) → Spring Data JPA → H2`.
Представления получают неизменяемые DTO, не JPA-сущности.
Товар, корзина и заказ разделены по пакетам. При покупке сохраняются
снимки названий и цен (изменение товара позже не влияет на старый заказ).
