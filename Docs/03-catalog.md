# Модуль Catalog

## Зона ответственности

Каталог описывает услуги, по которым можно создавать заявки. `deadline` — нормативный срок выполнения;
в текущем проекте нужно зафиксировать единицу измерения как часы.

## Текущее состояние

- есть `ServiceCatalogEntity` и таблица `service_catalog`;
- есть обратная JPA-связь с заявками;
- есть пустой repository и интерфейс `CatalogService`;
- нет implementation, DTO, controller, пагинации и тестов.

## Что нужно исправить

1. `ServiceCatalogItemRepository` назван по старому имени сущности. Переименовать в `ServiceCatalogRepository`.
2. `CatalogService` возвращает сущности и принимает набор аргументов. Перейти на command/DTO.
3. `listAll()` без пагинации станет небезопасным при росте каталога.
4. Схема не позволяет мягко скрывать услугу. Удалять услугу с заявками нельзя из-за FK. Добавить
   `active boolean NOT NULL DEFAULT true` новой миграцией.
5. Для SLA лучше иметь снимок deadline в заявке. Иначе изменение услуги изменит смысл старых заявок.

## DTO

```java
public record CatalogItemRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        @NotNull @Positive Integer deadlineHours
) {
}

public record CatalogItemDto(
        Long id,
        String name,
        String description,
        Integer deadlineHours,
        boolean active
) {
}
```

## План работ

1. Добавить `active` в БД и сущность.
2. Переименовать repository и добавить поиск по названию/active.
3. Создать request/response DTO и mapper.
4. Реализовать read-only поиск для всех ролей.
5. Реализовать create/update/deactivate для `ADMIN`.
6. При создании заявки разрешать только active-услуги.

## Пример сервиса

```java
@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final ServiceCatalogRepository repository;

    public Page<CatalogItemDto> find(String query, boolean includeInactive, Pageable pageable) {
        Page<ServiceCatalogEntity> page = includeInactive
                ? repository.findByNameServiceContainingIgnoreCase(query, pageable)
                : repository.findByNameServiceContainingIgnoreCaseAndActiveTrue(query, pageable);
        return page.map(CatalogMapper::toDto);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogItemDto create(CatalogItemRequest request) {
        if (repository.existsByNameServiceIgnoreCase(request.name())) {
            throw new ConflictException("SERVICE_NAME_EXISTS", "Service name already exists");
        }

        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setNameService(request.name().trim());
        entity.setDescription(request.description());
        entity.setDeadline(request.deadlineHours());
        entity.setActive(true);
        return CatalogMapper.toDto(repository.save(entity));
    }
}
```

`active` и его setter из примера ещё нет в текущем коде: сначала нужна Flyway-миграция.

## Тесты

- `deadlineHours <= 0` отклоняется;
- дублирующееся имя возвращает `409`;
- обычный пользователь не может менять каталог;
- деактивированная услуга не доступна для новой заявки;
- поиск и пагинация работают в стабильном порядке.

## Детальная декомпозиция задач

### CAT-01. Добавить мягкую деактивацию

Цель: не удалять услугу, на которую уже ссылаются requests.

Шаги:

1. Создать новую migration `ALTER TABLE service_catalog ADD COLUMN active boolean NOT NULL DEFAULT true`.
2. Добавить field/getter/setter в entity; не трогать `V1`.
3. Убедиться, что Hibernate validate проходит.
4. Не добавлять `CascadeType.REMOVE` к `requests`.

Приёмка: existing rows active; service с requests можно deactivate; старые requests продолжают отображать service.

### CAT-02. Переименовать repository и добавить queries

1. `ServiceCatalogItemRepository` → `ServiceCatalogRepository`.
2. Добавить `existsByNameServiceIgnoreCase`.
3. Добавить paged search active/all.
4. Добавить `findByIdAndActiveTrue` для request creation.
5. Для search нормализовать blank query: не строить разные запросы с `null` в controller.

Тест: exact/partial/case-insensitive search, active filter, stable sort, empty page.

### CAT-03. DTO, mapper и Validation

Файлы: `CreateCatalogItemRequest`, `UpdateCatalogItemRequest`, `CatalogItemDto`, `CatalogItemSummaryDto`, `CatalogMapper`.

Правила:

- name trim, 1–255, не пустая строка;
- description nullable, max в API определить явно;
- deadline hours positive; договорённость о часах отразить в Java name и OpenAPI;
- collections `requests` не маппить;
- `CatalogItemSummaryDto` использовать в `RequestDto`.

### CAT-04. Реализовать service

Методы:

- public `findActive(query, pageable)`;
- admin `findAll(query, active, pageable)`;
- `getById` с учётом роли/inactive;
- admin `create`, `update`, `deactivate`, при необходимости `activate`.

При update проверять duplicate name с исключением текущего ID. Не ловить только generic `DataIntegrityViolationException`: превращать известный
unique conflict в `409`, но не скрывать все ошибки БД.

### CAT-05. REST controller

1. GET collection с pagination и белым списком sort fields.
2. GET item; inactive item виден admin, а обычному user возвращает `404`.
3. POST — `201` + `Location`.
4. PATCH — команда должна выбрать semantics partial update; не путать absent и explicit null.
5. DELETE — soft deactivate, `204`; повтор идемпотентен.
6. Все mutating endpoints — admin + CSRF.

### CAT-06. SLA snapshot для Requests

Задача на стыке с REQ-02/DSH-01.

Команда должна решить:

- календарные или рабочие часы;
- от какого момента идёт SLA: create, approval или assignment;
- приостанавливается ли SLA на approval;
- нужен ли праздничный календар.

Для MVP: календарные часы от `createdAt`; сохранить `deadline_hours_snapshot` и `due_at` в request.

### CAT-07. Приёмочный набор тестов

- repository search + PostgreSQL collation expectations;
- service duplicate/update/deactivate/reactivate;
- controller validation + role matrix + CSRF;
- request creation отклоняет inactive service;
- historical request отображает inactive service;
- deadline snapshot не меняется после update catalog.
