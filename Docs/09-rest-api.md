# REST API Service Desk

## Статус документа

Это целевой контракт API, а не описание уже работающих endpoint. Контроллеров в проекте пока нет. При изменении
контракта нужно одновременно обновлять этот файл, DTO и MockMvc-тесты.

## Общие правила

- Base path: `/api/v1`.
- Content type: `application/json`, кроме form login и SSE.
- Время: ISO 8601 с offset, например `2026-11-01T12:30:00Z`.
- ID: JSON number, Java `Long`; ID статуса/роли может быть `Short` внутри backend.
- Пустые optional-поля передавать `null` или не передавать; не использовать `0` как пустой ID.
- JPA entities не являются API-моделями.

## Аутентификация и CSRF

Целевой вариант соответствует текущему Spring Security: session cookie и form login.

### Вход

```http
POST /login
Content-Type: application/x-www-form-urlencoded

username=teacher&password=secret
```

После успешного входа клиент сохраняет session cookie. Для browser frontend нужно отключить redirect и вернуть `204 No Content`
или JSON с профилем. Точное поведение зафиксировать в security-тестах.

### Текущий профиль

```http
GET /api/v1/auth/me
```

```json
{
  "id": 42,
  "login": "teacher",
  "role": "TEACHER",
  "departmentId": 3
}
```

### Выход

```http
POST /logout
X-XSRF-TOKEN: <token>
```

Ответ: `204 No Content`.

Для всех POST/PATCH/DELETE при session authentication frontend передаёт CSRF-токен. Имя cookie/header должно совпадать с
настройкой `CookieCsrfTokenRepository`.

## Пагинация

Параметры:

- `page` — с нуля;
- `size` — по умолчанию 20, максимум 100;
- `sort` — например `createdAt,desc`.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Не возвращать Spring `PageImpl` напрямую: его JSON не является стабильным API-контрактом. Ввести свой `PageResponse<T>`.

## Ошибки

```json
{
  "code": "REQUEST_INVALID_TRANSITION",
  "message": "Transition from ASSIGNED to DONE is not allowed",
  "fieldErrors": {},
  "timestamp": "2026-11-01T12:30:00Z"
}
```

| HTTP | Когда |
| --- | --- |
| `400 Bad Request` | Невалидный JSON/параметры |
| `401 Unauthorized` | Нет аутентификации |
| `403 Forbidden` | Нет роли или доступа к объекту |
| `404 Not Found` | Ресурс не найден или не виден пользователю |
| `409 Conflict` | Дубль, неверный status transition, optimistic lock |
| `422 Unprocessable Entity` | Команда синтаксически валидна, но нарушает бизнес-правило |

Команде нужно выбрать, различать ли `409` и `422`. Если нет, все domain conflicts можно единообразно отдавать как `409`.

## Users и справочники

### Users

| Method | Path | Доступ | Описание |
| --- | --- | --- | --- |
| GET | `/api/v1/users` | `ADMIN` | Поиск и пагинация |
| POST | `/api/v1/users` | `ADMIN` | Создать учётную запись |
| GET | `/api/v1/users/{id}` | `ADMIN`, уполномоченные | Получить профиль |
| PATCH | `/api/v1/users/{id}` | `ADMIN` | Изменить ФИО/роль/отдел |
| PUT | `/api/v1/users/{id}/password` | `ADMIN` или сам пользователь | Изменить пароль |
| GET | `/api/v1/departments` | authenticated | Справочник отделов |
| GET | `/api/v1/roles` | `ADMIN` | Справочник ролей |

```http
POST /api/v1/users
Content-Type: application/json
X-XSRF-TOKEN: <token>
```

```json
{
  "login": "ivanov",
  "password": "strong-password",
  "name": "Иван",
  "lastName": "Иванов",
  "patronymic": "Иванович",
  "roleId": 1,
  "departmentId": 3
}
```

Ответ: `201 Created`, header `Location: /api/v1/users/{id}` и `UserDto` без password/passwordHash.

## Catalog

| Method | Path | Доступ | Описание |
| --- | --- | --- | --- |
| GET | `/api/v1/catalog` | authenticated | Поиск active-услуг |
| GET | `/api/v1/catalog/{id}` | authenticated | Одна услуга |
| POST | `/api/v1/catalog` | `ADMIN` | Создать услугу |
| PATCH | `/api/v1/catalog/{id}` | `ADMIN` | Изменить услугу |
| DELETE | `/api/v1/catalog/{id}` | `ADMIN` | Мягко деактивировать |

Фильтры GET: `query`, `active`, `page`, `size`, `sort`. Обычный пользователь не должен видеть inactive-услуги.

```json
{
  "name": "Ремонт проектора",
  "description": "Диагностика и ремонт",
  "deadlineHours": 24
}
```

## Audiences

| Method | Path | Доступ | Описание |
| --- | --- | --- | --- |
| GET | `/api/v1/audiences` | authenticated | Поиск аудитории |
| POST | `/api/v1/audiences` | `ADMIN` | Создать |
| PATCH | `/api/v1/audiences/{id}` | `ADMIN` | Переименовать |

Удалять аудиторию с историческими заявками нельзя. Если нужно скрывать аудитории, добавить `active` в схему.

## Requests

| Method | Path | Описание |
| --- | --- | --- |
| GET | `/api/v1/requests` | Список с ролевой видимостью |
| POST | `/api/v1/requests` | Создать заявку от имени CurrentUser |
| GET | `/api/v1/requests/{id}` | Детали заявки |
| GET | `/api/v1/requests/{id}/history` | История статусов |
| GET | `/api/v1/requests/{id}/checklist` | Чек-лист |

Фильтры: `status`, `authorId`, `executorId`, `departmentId`, `serviceId`, `createdFrom`, `createdTo`, `page`, `size`, `sort`.
Контроллер не должен доверять фильтру `authorId`: допустимый scope всё равно задаётся ролью.

### Создание

```http
POST /api/v1/requests
```

```json
{
  "audienceId": 12,
  "serviceId": 5,
  "description": "Не включается проектор"
}
```

Ответ: `201 Created`, `Location: /api/v1/requests/{id}`.

```json
{
  "id": 101,
  "author": {
    "id": 42,
    "name": "Иван Иванов"
  },
  "audience": {
    "id": 12,
    "number": "А-301"
  },
  "service": {
    "id": 5,
    "name": "Ремонт проектора",
    "deadlineHours": 24
  },
  "status": "PENDING_HEAD",
  "description": "Не включается проектор",
  "approver": null,
  "executor": null,
  "createdAt": "2026-11-01T12:30:00Z",
  "approved": null,
  "rejectReason": null,
  "version": 0
}
```

## Workflow commands

| Method | Path | Роль |
| --- | --- | --- |
| POST | `/api/v1/requests/{id}/actions/approve-by-head` | `DEPARTMENT_HEAD` |
| POST | `/api/v1/requests/{id}/actions/reject-by-head` | `DEPARTMENT_HEAD` |
| POST | `/api/v1/requests/{id}/actions/assign` | `DEPARTMENT_STAFF` |
| POST | `/api/v1/requests/{id}/actions/reject-by-department` | `DEPARTMENT_STAFF` |
| POST | `/api/v1/requests/{id}/actions/start` | Назначенный `DUTY_OFFICER` |
| POST | `/api/v1/requests/{id}/actions/complete` | Назначенный `DUTY_OFFICER` |
| POST | `/api/v1/requests/{id}/actions/cancel` | Автор или `ADMIN` |

Команды сознательно оформлены как actions, а не как произвольное `PATCH status`: так API выражает бизнес-намерение.

Approve:

```json
{
  "expectedVersion": 0
}
```

Reject:

```json
{
  "reason": "Заявка не относится к нашему отделу",
  "expectedVersion": 0
}
```

Assign:

```json
{
  "executorId": 77,
  "expectedVersion": 1
}
```

Успешная action возвращает обновлённый `RequestDto`. Если `expectedVersion` устарел, возвращается `409 Conflict`.

## Checklists

| Method | Path | Описание |
| --- | --- | --- |
| GET | `/api/v1/requests/{requestId}/checklist` | Чек-лист заявки |
| PUT | `/api/v1/requests/{requestId}/checklist/{itemId}` | Изменить completed |
| GET | `/api/v1/catalog/{serviceId}/checklist-template` | Шаблон |
| POST | `/api/v1/catalog/{serviceId}/checklist-template/items` | `ADMIN`: добавить item |
| PATCH | `/api/v1/catalog/{serviceId}/checklist-template/items/{itemId}` | `ADMIN`: изменить item |

```json
{
  "completed": true
}
```

## Notifications

| Method | Path | Описание |
| --- | --- | --- |
| GET | `/api/v1/notifications` | Свои notifications, пагинация |
| GET | `/api/v1/notifications/unread-count` | Число непрочитанных |
| PUT | `/api/v1/notifications/{id}/read` | Отметить прочитанным |
| PUT | `/api/v1/notifications/read-all` | Отметить все |
| GET | `/api/v1/notifications/stream` | SSE-поток |

Фильтры: `unreadOnly`, `page`, `size`. `userId` никогда не принимается из HTTP.

SSE:

```http
GET /api/v1/notifications/stream
Accept: text/event-stream
```

```text
event: notification
id: 123
data: {"id":123,"type":"REQUEST_ASSIGNED","title":"Новая заявка","requestId":101}
```

## Dashboard

```http
GET /api/v1/dashboard?from=2026-10-01&to=2026-11-01&departmentId=3&serviceId=5
```

Доступ: `ADMIN`, `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF`, ограниченно `DUTY_OFFICER`.

```json
{
  "open": 25,
  "unassigned": 4,
  "overdue": 3,
  "byStatus": {
    "PENDING_HEAD": 2,
    "PENDING_DEPARTMENT": 3,
    "ASSIGNED": 8,
    "IN_PROGRESS": 12
  },
  "byExecutor": [
    {
      "executorId": 77,
      "executorName": "Пётр Петров",
      "activeRequests": 6
    }
  ]
}
```

## OpenAPI

После появления controllers стоит добавить `springdoc-openapi-starter-webmvc-ui` с версией, совместимой с Spring Boot 3.5. OpenAPI должен
генерироваться из реальных DTO/controllers, а CI должен сохранять `openapi.json` как артефакт. Swagger UI в dev-профиле
можно открыть, но в production его нужно либо защитить, либо отключить.

## API Definition of Done

- каждый endpoint имеет request/response DTO;
- на входе работает `@Valid`;
- success/error status покрыты MockMvc-тестами;
- есть тест на `401`, `403` и CSRF;
- чужие данные не раскрываются;
- пароль/password hash никогда не попадает в JSON/log;
- OpenAPI совпадает с этим контрактом;
- breaking changes не вносятся в `/api/v1` без согласования.

## Детальный план реализации API

### API-01. Создать common web-слой

Цель: чтобы каждый модуль не изобретал свой формат page/error.

Создать в `ru.miit.servicedesk.common`:

- `ApiError`;
- `FieldValidationError` или map field/message;
- `PageResponse<T>`;
- base domain exceptions: not found, conflict, forbidden/domain validation;
- `ApiExceptionHandler`;
- factory для `Location` URI, если команда сочтёт полезным.

Не ловить `Exception.class` и не возвращать `200` с error body. Не показывать SQL, stack trace, class names и internal path.

Маппинг:

- `MethodArgumentNotValidException` → 400 + field errors;
- malformed JSON/type mismatch → 400;
- not found → 404;
- access denied → 403 (security handlers отдельно);
- unique/optimistic/domain conflict → 409;
- unknown exception → 500 с correlation ID, полный stack только в server log.

Приёмка: MockMvc-тест на каждый mapping и стабильный JSON schema.

### API-02. Стандартизировать pagination/filter/sort

1. Создать mapper Spring `Page<T>` → `PageResponse<R>`.
2. Глобально ограничить size 1–100; default 20.
3. Каждый controller имеет whitelist sort fields; не передавать произвольное property path в JPA.
4. Blank query нормализовать в null/empty одинаково.
5. Date ranges всегда half-open внутри service, чтобы не потерять конец дня.
6. Неверный enum/date/sort → 400 с понятным field error.

Тесты: defaults, max+1, negative page, unknown sort, first/last/empty page, stable tie-breaker by ID.

### API-03. Зафиксировать controller conventions

Для каждого controller:

1. `@RestController`, `@RequestMapping("/api/v1/...")`.
2. Request DTO + `@Valid`; не `Map<String, Object>`.
3. Controller делегирует в service и не открывает transaction.
4. POST create → 201 + Location + response DTO.
5. GET → 200; empty collection → 200, не 404.
6. Update action → 200 с updated DTO; idempotent command может 204.
7. Delete/soft deactivate → 204.
8. Не оборачивать всё в `ResponseEntity`, если статус фиксирован; можно `@ResponseStatus`.
9. Не логировать request body с password/reason/sensitive text без masking.

### API-04. Реализовать Users/Auth endpoints

Зависит от USR-04/05 и SEC-01–06.

Чек-лист:

- form login success/failure JSON behavior;
- `/auth/me`;
- logout;
- users CRUD/password;
- departments/roles reference endpoints;
- role matrix;
- CSRF for mutations;
- no password leakage;
- OpenAPI examples.

Ручная приёмка: login cookie → auth/me → admin creates user → new user logs in → wrong role denied.

### API-05. Реализовать Catalog/Audience endpoints

Зависит CAT-05, REQ-01.

Проверить:

- public authenticated list показывает только active;
- admin видит active filter/all;
- duplicate errors stable;
- soft delete, не physical;
- pagination и search;
- validation lengths совпадают с DB.

### API-06. Реализовать Requests read/create endpoints

Зависит REQ-06–08.

Чек-лист:

- create не принимает author/status/executor/approved;
- GET list scope ограничен в repository/service;
- GET by ID чужого не раскрывает существование;
- summary/details DTO отличаются осознанно;
- history отдельно;
- no lazy serialization;
- query count приемлем.

### API-07. Реализовать Workflow actions

Зависит WF-01–10.

Для каждой action:

1. Отдельный endpoint и DTO.
2. `expectedVersion` required.
3. Actor не принимается.
4. Reason validation, где нужно.
5. Role + scope + status + business guard.
6. Updated RequestDto response.
7. Invalid status/version → stable 409 code.
8. History/event verified integration test.

Обязательно один end-to-end test всей цепочки.

### API-08. Checklists/Notifications/Dashboard endpoints

Реализовывать после domain services, не начинать с controller stub.

- checklist: visibility request + executor mutation;
- notification: current user implicit, no userId parameter;
- SSE: correct media type, timeout cleanup;
- dashboard: role scope cannot be overridden by query department;
- all: consistent error/page format.

### API-09. Добавить OpenAPI без расхождения с кодом

1. Подобрать springdoc version по official compatibility matrix.
2. Описать session cookie/CSRF security scheme или ясно документировать в description.
3. Добавить `@Schema` только где Java не даёт нужного описания.
4. Добавить response/error examples для actions.
5. Генерировать spec в integration/CI и проверять diff.
6. Swagger UI не permitAll в production без осознанного решения.

### API-10. Собрать приёмочную матрицу

Для каждой строки endpoint-таблицы должны быть:

- happy-path test;
- malformed JSON;
- validation error;
- anonymous;
- wrong role;
- wrong object scope;
- not found;
- conflict, если применим;
- CSRF для mutation;
- response contract assertions, не только status;
- проверка, что service вызван с нужным command/current actor.

Отдельный smoke-test после `docker compose up -d --build app`:

1. health/startup без Hibernate/Flyway error;
2. login;
3. auth/me;
4. catalog/audience list;
5. create request;
6. read request;
7. logout;
8. old session no longer authorized.
