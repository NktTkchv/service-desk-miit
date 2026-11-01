# План развития Service Desk

Этот каталог содержит учебный план реализации проекта. Гайды описывают желаемую
архитектуру, шаги разработки, примеры кода и критерии готовности. Примеры не нужно копировать
механически: команда должна обсудить бизнес-правила и покрыть их тестами.

## Гайды по модулям

1. [Users](01-users.md) — пользователи, роли и подразделения.
2. [Security](02-security.md) — вход, авторизация, CSRF и ограничение доступа.
3. [Catalog](03-catalog.md) — каталог услуг и SLA.
4. [Requests](04-requests.md) — заявки, аудитории и статусы.
5. [Workflow](05-workflow.md) — согласование и переходы между статусами.
6. [Checklists](06-checklists.md) — планируемый модуль чек-листов.
7. [Notifications](07-notifications.md) — внутренние, email- и чат-уведомления.
8. [Dashboard](08-dashboard.md) — агрегаты и отчёты.
9. [REST API](09-rest-api.md) — целевой HTTP-контракт между backend и frontend.
10. [Инфраструктура и тесты](10-infrastructure-and-testing.md) — общие задачи сборки, CI, Testcontainers и Docker.

Полные copy-ready листинги целевой реализации собраны в [Code](Code/README.md). Они намеренно не перенесены в `src`:
команда должна реализовывать задачи поэтапно, понимая каждую строку и проверяя тестами.

## Рекомендуемый порядок работ

```text
users → security → catalog → requests → workflow
                                      ├─→ checklists
                                      ├─→ notifications
                                      └─→ dashboard
```

1. Сначала договориться о DTO, ошибках API, ролях и именах статусов.
2. Реализовать users и security, чтобы остальной API можно было защищать сразу.
3. Заполнить каталог и справочник аудиторий.
4. Реализовать создание и чтение заявок.
5. Зафиксировать workflow как единственное место изменения статуса.
6. Подключить чек-листы и события.
7. Подписать notifications на события, затем сделать dashboard.

## Как пользовать эти гайды как backlog

Каждый модуль разбит на задачи с идентификаторами: `USR-*`, `SEC-*`, `CAT-*`, `REQ-*`, `WF-*`, `CHK-*`, `NTF-*`,
`DSH-*`, `API-*`. Один pull request должен закрывать одну задачу или несколько неразделимых мелких подзадач.

Перед началом задачи исполнитель:

1. Читает весь гайд модуля, а не только свой пункт.
2. Проверяет зависимости задачи и статус нужных миграций.
3. Уточняет нерешённые domain-вопросы, отмеченные в гайде.
4. Создаёт ветку с номером задачи, например `feature/REQ-04-create-request`.
5. Сначала пишет проверку бизнес-правила, затем implementation.
6. Обновляет REST API и гайд, если фактическая реализация отличается.

## Этапы выпуска

### Этап 1. Фундамент

- `USR-01`–`USR-06`: repository, DTO, mapper, service, admin API;
- `SEC-01`–`SEC-06`: principal, CurrentUser, method security, JSON errors, CSRF;
- `API-01`–`API-03`: общая ошибка, page response, controller conventions.

Результат: можно войти, получить `/auth/me`, создать учётную запись и проверить ролевой доступ.

### Этап 2. Справочники

- `CAT-01`–`CAT-06`: migration active, repository, DTO, CRUD, API;
- `REQ-01`: audience CRUD;
- dev seed data для departments, users, audiences и catalog.

Результат: в системе есть всё, что нужно для создания заявки.

### Этап 3. Базовые заявки

- `REQ-02`–`REQ-08`: migration, DTO, mapper, create, get, list, visibility;
- только создание и read operations, без обхода workflow.

Результат: teacher создаёт заявку и видит её, роли видят только свой scope.

### Этап 4. Workflow

- `WF-01`–`WF-09`: statuses, history, lock, approvals, assignment, execution, cancellation, events;
- `CHK-01`–`CHK-05`: снимок чек-листа и блокировка completion.

Результат: заявка проходит весь жизненный цикл, а каждое изменение имеет автора и время.

### Этап 5. Уведомления и отчёты

- `NTF-01`–`NTF-08`: storage, events, recipients, API, SSE, external channels;
- `DSH-01`–`DSH-06`: SLA snapshot, projections, filters, access, API.

Результат: пользователи видят изменения, а руководители — измеримые показатели.

## Правила для Flyway-задач

1. Номер миграции выбирается после синхронизации с main, чтобы две ветки не создали один `V2`.
2. Применённый файл никогда не редактируется. Исправление делается следующей миграцией.
3. Новая NOT NULL колонка для непустой таблицы добавляется поэтапно: nullable/default, backfill, constraint.
4. Каждый FK и query получают индекс, если это подтверждено сценарием чтения.
5. Миграция проверяется на чистой PostgreSQL и на базе с предыдущей версией.

## Шаблон описания pull request

```text
Задача: REQ-04

Что сделано:
- ...

Какие правила реализованы:
- ...

Миграции:
- V...__....sql / нет

Как проверить:
1. ...

Тесты:
- ./gradlew clean test

Оставшиеся вопросы:
- ...
```

## Общие правила реализации

- Не возвращать JPA-сущности из REST-контроллеров. Двунаправленные связи вызовут рекурсию
  JSON, а LAZY-связи — `LazyInitializationException`. На границе API нужны DTO.
- Входные DTO описывать Java `record` и проверять через Jakarta Validation.
- Публичные методы сервисов, меняющие данные, делать `@Transactional`.
- Не доверять `userId` из HTTP-запроса. Текущего пользователя получать из Spring Security.
- Все переходы статуса выполнять через workflow, а не прямым `request.setStatus(...)` из контроллера.
- Для каждой ошибки иметь стабильный код, HTTP-статус и читаемое сообщение.
- Не менять уже применённую `V1__init.sql`: каждое изменение БД должно быть новой миграцией.

## Базовый формат ошибки

```java
public record ApiError(
        String code,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
}
```

```java
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(EntityNotFoundException exception) {
        ApiError error = new ApiError(
                "ENTITY_NOT_FOUND",
                exception.getMessage(),
                Map.of(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

Общие DTO, исключения и `@RestControllerAdvice` лучше вынести в пакет `ru.miit.servicedesk.common`.

## Definition of Done для любого модуля

- код собирается командой `./gradlew clean test`;
- есть unit-тесты бизнес-правил;
- есть repository-тесты с PostgreSQL/Testcontainers для нетривиальных запросов;
- есть MockMvc-тесты успешных и ошибочных HTTP-сценариев;
- проверены роли и доступ к чужим данным;
- REST-контракт актуализирован в [09-rest-api.md](09-rest-api.md).
