# Инфраструктура, сборка и тесты

Этот гайд описывает общие задачи, которые не принадлежат одному domain-модулю, но нужны каждой команде.

## Текущее состояние

- тестовых Java-классов нет;
- `spring-boot-starter-test` есть, но `spring-security-test` и Testcontainers явно не добавлены;
- Gradle build компилирует проект;
- Dockerfile собирает bootJar в multi-stage build;
- Compose поднимает PostgreSQL, Redis и app;
- нет CI, coverage policy, lint/static analysis и application healthcheck.

## INF-01. Добавить тестовые dependencies

Проверить official/current versions перед добавлением. Минимум:

```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
```

Если версии Testcontainers не управляются Spring dependency management, добавить BOM. Не смешивать JUnit 4/5.

Приёмка: пустой context/repository security test запускается через `./gradlew test`.

## INF-02. Разделить уровни тестов

### Unit

- без Spring context;
- JUnit + Mockito;
- business rules, mapper, validators, state machine;
- milliseconds, большая часть branch coverage.

### Repository

- PostgreSQL Testcontainers, не H2: SQL/identity/timestamptz/partial index отличаются;
- Flyway migrations applied;
- repository query, constraints, mapping, locking;
- data cleanup transaction или container lifecycle.

### Web slice

- `@WebMvcTest`;
- mock service;
- JSON, Validation, error mapping, role, CSRF;
- не дублировать service business rules.

### Integration

- `@SpringBootTest` + Testcontainers;
- real repositories/services/security;
- end-to-end request lifecycle;
- медленнее, поэтому только ключевые цепочки.

## INF-03. Общий PostgreSQL container

Создать base test/configuration и `@DynamicPropertySource`:

```java
@Testcontainers
abstract class PostgreSqlIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

Не дублировать container в каждом test class, если это резко замедляет build. Не включать reuse в CI без понимания isolation.

## INF-04. Тестовые fixtures

Создать builders/factories, а не огромные SQL в каждом тесте:

- `UserTestFactory` с явной role/department;
- `CatalogTestFactory`;
- `RequestTestFactory` с status/author/executor;
- `SecurityTestUsers`/custom `@WithMock...`.

Правила:

- builder defaults valid, но каждый тест явно показывает важную разницу;
- не делить mutable entity между тестами;
- не завязываться на identity ID, если это не сам тест;
- Clock инжектить и фиксировать.

## INF-05. Ввести Clock

Добавить bean:

```java
@Bean
Clock clock() {
    return Clock.systemUTC();
}
```

В business services не вызывать `now()` напрямую. В tests — `Clock.fixed`. Hibernate/DB timestamps и injected Clock могут разойтись на миллисекунды;
не сравнивать exact без контроля source.

## INF-06. Профиль test

Создать `src/test/resources/application-test.yml`:

- Flyway enabled;
- `ddl-auto: validate`;
- external email/chat disabled/fake;
- async либо synchronous executor для deterministic tests, либо Awaitility;
- Redis container только для tests, которым он реально нужен;
- никаких production credentials.

Не подменять PostgreSQL H2 для repository tests.

## INF-07. Добавить application health

Вариант: Spring Boot Actuator.

1. Добавить dependency.
2. Открыть только `/actuator/health` без sensitive details anonymous, если endpoint нужен Docker.
3. Добавить app healthcheck в Compose.
4. `depends_on` для app уже ждёт DB/Redis; health app позволит проверить готовность и Flyway.
5. Не открывать env/config/beans/loggers endpoints anonymous.

## INF-08. Улучшить Docker build

1. Проверить, почему Dockerfile использует Gradle 8.14, а wrapper — 9.6; выровнять версию.
2. Копировать wrapper и запускать `./gradlew`, чтобы local/CI/Docker использовали одну версию.
3. Разделить COPY build files/source для Docker layer cache.
4. Создать `.dockerignore`: `.gradle`, `build`, `.git`, `.idea`, `.env`.
5. Runtime запускать non-root user.
6. Не копировать secrets в image layers.
7. Проверить graceful shutdown и memory flags после необходимости, не оптимизировать заранее.

Приёмка: clean `docker compose build app`, `docker compose up -d app`, health becomes healthy, logs без credentials.

## INF-09. CI pipeline

Минимальные jobs:

1. Checkout.
2. Java 21.
3. Gradle wrapper validation.
4. Dependency cache.
5. `./gradlew clean test`.
6. Публикация JUnit reports.
7. После появления — coverage/static analysis/OpenAPI generation.
8. Docker build smoke на main/PR по времени.

Не ставить coverage 100% как самоцель. Сначала покрыть state machine, access, transaction, mapping и errors.

## INF-10. Логи, correlation ID и secrets

1. Добавить request correlation ID filter: принимать валидный header или генерировать.
2. Положить в MDC, вернуть response header, очищать finally.
3. Логировать workflow action: request ID, actor ID, action, result; не логировать password/full sensitive description.
4. `.env` остаётся gitignored; `.env.example` без secrets.
5. В production credentials передаются средой/secrets manager, не в image и не в Git.

## INF-11. Финальная приёмка релиза

- clean Gradle build;
- all tests, reports saved;
- all Flyway migrations проходят clean + upgrade;
- Docker image builds without local build artifacts;
- Compose app healthy;
- smoke API lifecycle passes;
- no secrets in repository/image/logs;
- docs/API updated;
- rollback/backup plan описан перед production migration.
