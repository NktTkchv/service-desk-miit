# Модуль Dashboard

## Зона ответственности

Dashboard строит read-only агрегаты по заявкам: количество открытых, распределение по статусам/исполнителям,
просроченные заявки и SLA. Он не меняет domain entities.

## Текущее состояние

`DashboardService` содержит только `countOpenRequests()` и `requestsByExecutor()`. Нет реализации, DTO, фильтров и API.

## Что нужно исправить

1. `Map<Long, Long>` не объясняет, что означают key/value, и не содержит имя executor. Нужен typed DTO.
2. `countOpenRequests()` не определяет, какие статусы считаются открытыми. Исключить `DONE`, `REJECTED`, `CANCELLED`.
3. Нет фильтра по периоду, department и service.
4. Текущая схема не хранит deadline snapshot/due date, поэтому исторический SLA посчитать надёжно нельзя.
5. Агрегацию нужно делать SQL/JPQL-запросом, а не загрузкой всех entities в Java.

## DTO

```java
public record DashboardFilter(
        LocalDate from,
        LocalDate to,
        Long departmentId,
        Long serviceId
) {
}

public record ExecutorLoadDto(
        Long executorId,
        String executorName,
        long activeRequests
) {
}

public record DashboardDto(
        long open,
        long unassigned,
        long overdue,
        Map<String, Long> byStatus,
        List<ExecutorLoadDto> byExecutor
) {
}
```

## План работ

1. Добавить `due_at`/`deadline_hours_snapshot` в request новой миграцией.
2. Заполнять due date при создании request.
3. Создать projection interfaces/records для aggregate queries.
4. Реализовать filters и правила видимости по роли.
5. Добавить индексы после `EXPLAIN ANALYZE`, а не заранее наугад.

## Пример projection и query

```java
public interface StatusCountProjection {

    String getStatus();

    long getCount();
}
```

```java
@Query("""
        select status.name as status, count(request.id) as count
        from RequestEntity request
        join request.status status
        where request.createdAt >= :from
          and request.createdAt < :to
        group by status.name
        order by status.name
        """)
List<StatusCountProjection> countByStatus(
        OffsetDateTime from,
        OffsetDateTime to
);
```

Для optional-фильтров лучше собирать Criteria query/Querydsl или иметь несколько явных запросов. Не строить JPQL строковой
конкатенацией из HTTP-параметров.

## Права доступа

- `ADMIN` — вся система;
- `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF` — только свой department;
- `DUTY_OFFICER` — своя нагрузка и доступные ему operational metrics;
- `TEACHER` — не имеет доступа к управленческому dashboard.

## Тесты

- terminal statuses не входят в open;
- request без executor входит в unassigned;
- границы периода работают без double count;
- отделы не видят данные друг друга;
- пустая база возвращает нули и пустые коллекции;
- query count не растёт как N+1.

## Детальная декомпозиция задач

### DSH-01. Определить метрики до SQL

Для каждой метрики записать формулу:

- open: какие statuses;
- unassigned: executor null и какие statuses;
- overdue: `now > dueAt` и status non-terminal;
- average resolution: `DONE timestamp - createdAt` или assignment;
- SLA compliance: completed before dueAt, что делать с open overdue;
- executor load: ASSIGNED + IN_PROGRESS или все non-terminal.

Нужно определить timezone границ `from/to`. Рекомендация: API LocalDate в business timezone, service преобразует в UTC half-open
interval `[fromStart, dayAfterToStart)`.

### DSH-02. Добавить SLA data

Зависит от CAT-06/REQ-02.

1. Добавить snapshot/dueAt migration/entity fields.
2. При completion history даёт timestamp DONE. Для упрощения можно добавить `completed_at` в request, но не дублировать без правила.
3. Исторический dashboard не читает current catalog deadline.
4. Тест границы exactly dueAt.

### DSH-03. DTO/filter/projections

Создать:

- validated `DashboardFilter`;
- `DashboardDto`;
- projections `StatusCount`, `ExecutorLoad`, `SlaSummary`;
- mapper/factory, который добавляет zero для отсутствующих statuses, если UI нужен стабильный набор keys.

Валидация: from <= to; максимальный период для тяжёлого dashboard; service/department existence проверять или пустой result — выбрать.

### DSH-04. Repository aggregate queries

1. Один query на status counts.
2. Один query на executor load с left join, если нужны zero-load officers.
3. Query на open/unassigned/overdue может вернуть conditional aggregates.
4. Все queries принимают scope department/user, не фильтруются после.
5. Не возвращать entities и aggregate `Object[]`; typed projection only.
6. Проверить generated SQL и `EXPLAIN ANALYZE` на тестовом объёме.

Возможные indexes добавлять после plan: `(status_id, created_at)`, `(executor_id, status_id)`, due_at partial for non-terminal нельзя сделать с dynamic status names без денормализации.

### DSH-05. Service и role scope

1. ADMIN — принимает optional department.
2. HEAD/STAFF — department жёстко берётся из CurrentUser; query parameter departmentId игнорируется или forbidden.
3. DUTY_OFFICER — отдельный personal dashboard, а не management metrics.
4. TEACHER — `403`.
5. Service собирает queries в DTO в read-only transaction.

Не кэшировать до измерения. Если нужен Redis cache, key включает role scope + все filters; TTL короткий; stale data допустимы только по решению.

### DSH-06. Controller и tests

Controller принимает dates/optional IDs, валидирует format, не строит SQL. Response stable даже для empty DB.

Тестовый dataset должен содержать:

- по одной request каждого status;
- overdue/not overdue/exactly due;
- unassigned;
- 2 departments, 2 services, 2 executors;
- rows на границах from/to;
- DONE before/after SLA.

Проверить каждую цифру в ответе, role scopes и query count/performance.
