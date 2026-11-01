# Модуль Workflow

## Зона ответственности

Workflow — единственное место, где можно менять статус, согласующего и исполнителя заявки. Он проверяет
текущий статус, роль актора, его отдел, обязательные поля и записывает историю.

## Что уже есть

Только `ApprovalService` с четырьмя методами. Реализации, истории и явной модели переходов нет.

## Что нужно исправить

1. `headId` и `staffId` не должны приходить от клиента. Актора нужно получать из `CurrentUser`.
2. Методы описывают только согласования. Нет `start`, `complete`, `cancel`, повторного назначения.
3. Нет истории статусов и optimistic locking.
4. Нет однозначного правила выбора руководителя отдела. Схема содержит только role и department.
5. `is_approved` не описывает, какой из двух этапов пройден. Надёжнее полагаться на status и history.

## Команды workflow

```java
public record RejectRequestCommand(
        @NotBlank @Size(max = 2000) String reason,
        @NotNull Long expectedVersion
) {
}

public record AssignRequestCommand(
        @NotNull Long executorId,
        @NotNull Long expectedVersion
) {
}
```

`expectedVersion` позволяет отклонить команду, если UI работает с устаревшей версией заявки.

## План работ

1. Создать `RequestStatus` enum с именами, точно совпадающими с БД.
2. Описать матрицу разрешённых переходов.
3. Добавить history entity/repository и `@Version` к `RequestEntity`.
4. Создать один `RequestWorkflowService`, а `ApprovalService` либо удалить, либо оставить фасадом.
5. Все операции выполнять в одной транзакции: lock/check/change/history/event.
6. Публиковать domain event. Обработчики notifications должны срабатывать только после commit.

## Пример матрицы

```java
private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = Map.of(
        PENDING_HEAD, Set.of(PENDING_DEPARTMENT, REJECTED, CANCELLED),
        PENDING_DEPARTMENT, Set.of(ASSIGNED, REJECTED, CANCELLED),
        ASSIGNED, Set.of(IN_PROGRESS, CANCELLED),
        IN_PROGRESS, Set.of(DONE, CANCELLED),
        DONE, Set.of(),
        REJECTED, Set.of(),
        CANCELLED, Set.of()
);

private void requireTransition(RequestStatus from, RequestStatus to) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
        throw new InvalidTransitionException(from, to);
    }
}
```

## Пример согласования

```java
@Transactional
@PreAuthorize("hasRole('DEPARTMENT_HEAD')")
public RequestDto approveByHead(Long requestId, long expectedVersion) {
    RequestEntity request = requestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Request not found"));

    requireVersion(request, expectedVersion);
    requireSameDepartment(currentUser.departmentId().orElseThrow(), request.getAuthor());
    requireStatus(request, PENDING_HEAD);

    RequestStatusEntity next = statusRepository.getRequired(PENDING_DEPARTMENT.name());
    RequestStatusEntity previous = request.getStatus();
    request.setApprover(userRepository.getReferenceById(currentUser.id()));
    request.setStatus(next);
    historyRepository.save(RequestStatusHistoryEntity.of(
            request, previous, next, currentUser.id(), null
    ));
    eventPublisher.publishEvent(new RequestStatusChangedEvent(
            request.getId(), previous.getName(), next.getName(), currentUser.id()
    ));
    return RequestMapper.toDto(request);
}
```

`getRequired`, history entity, `requireVersion` и event в текущем коде отсутствуют: это целевой пример.

## Правила ролей

| Команда | Роль | Дополнительное правило |
| --- | --- | --- |
| approve/reject by head | `DEPARTMENT_HEAD` | Автор в том же отделе |
| assign/reject by department | `DEPARTMENT_STAFF` | Заявка отдела сотрудника |
| start/complete | `DUTY_OFFICER` | Он текущий executor |
| cancel | Автор или `ADMIN` | Только из разрешённых статусов |

## Тесты

- каждый разрешённый и запрещённый переход;
- чужой отдел и неверная роль;
- executor не может менять чужую заявку;
- reject без reason отклоняется;
- для каждого перехода появляется ровно одна history-запись;
- event не обрабатывается при rollback;
- устаревшая version даёт `409`.

## Детальная декомпозиция задач

### WF-01. Зафиксировать state machine

До кода команда должна письменно ответить:

1. Какой initial status?
2. Кто и из каких статусов может cancel?
3. Можно ли reassign executor?
4. Можно ли reopen DONE/REJECTED?
5. Нужен ли reason для cancel/reassign?
6. Когда проверяется checklist?

Результат: таблица from/to/action/role/guard/side effects в этом гайде. Без этого WF-04+ не начинать.

### WF-02. Создать status enum и resolver

1. Enum с точными DB names.
2. `RequestStatusResolver.get(RequestStatus)` использует repository cache или lookup.
3. Не зашивать numeric ID статуса: identity ID может отличаться.
4. На startup можно проверить, что в БД есть все enum values и нет unknown.

### WF-03. History и optimistic locking

Зависит от: REQ-02/REQ-03.

Реализовать `RequestStatusHistoryEntity`, repository, DTO и read service. History immutable: не давать update/delete API. Каждая workflow-команда пишет:

- request;
- previous/new status;
- actor;
- UTC timestamp;
- reason, если есть;
- при необходимости metadata JSON, но для MVP лучше без неформализованного JSON.

Версию сравнивать до mutation. Дополнительно JPA `@Version` защитит commit race. `ObjectOptimisticLockingFailureException` маппить в `409`.

### WF-04. Создать единый transition helper

Чтобы не копировать history/event в каждом методе, создать private/domain helper:

```text
transition(request, target, actor, reason)
  → check matrix
  → remember previous
  → set target
  → create history
  → register event
```

Helper не проверяет role-specific scope — это делает action method до transition. Event должен содержать ID/snapshot values, а не entity/proxy.

### WF-05. Head approval/rejection

Зависит от: USR-06, SEC-03/04, WF-01–04.

Approve guards:

- actor role HEAD;
- request status PENDING_HEAD;
- actor department exists and equals author department;
- expected version matches;
- request not terminal.

Effects: approver = actor; status PENDING_DEPARTMENT; `approved` semantics определить; history; event.

Reject guards те же + nonblank reason. Effects: approver actor, REJECTED, approved false, rejectReason, history/event.

Тесты: same/foreign/null department, wrong status, wrong role, blank reason, concurrent approval.

### WF-06. Department assignment/rejection

Assign guards:

- actor STAFF (или также HEAD — решить);
- request PENDING_DEPARTMENT;
- same department;
- executor exists, enabled, role DUTY_OFFICER;
- expected version.

Effects: executor, ASSIGNED, clear previous rejection fields if semantics allows, history/event.

Reassign лучше отдельной action: allowed status, reason, old/new executor в event/history. Не подменять её повторным assign без audit.

### WF-07. Start и complete

Start:

- actor DUTY_OFFICER и actor == executor;
- ASSIGNED → IN_PROGRESS;
- expected version, history, event.

Complete:

- actor == executor;
- IN_PROGRESS → DONE;
- required checklist complete;
- если checklist template пуст, completion allowed;
- history/event.

Не принимать executorId из HTTP для start/complete.

### WF-08. Cancellation

Нужно решение WF-01. Рекомендуемый MVP:

- author: cancel PENDING_HEAD/PENDING_DEPARTMENT;
- admin: cancel any non-terminal status;
- reason required после assignment, optional before;
- terminal statuses не меняются;
- repeated cancel либо idempotent success, либо `409` — зафиксировать.

### WF-09. Events и transaction boundary

1. Создать events `RequestCreated`, `RequestStatusChanged`, `RequestAssigned`, возможно `RequestCompleted`.
2. Событие содержит IDs, status names, actor, timestamp; не entities.
3. Публиковать в transaction, обрабатывать notifications AFTER_COMMIT.
4. Не ловить exception listener так, чтобы она стала невидимой; лог/metric/retry.
5. Для guaranteed delivery позже outbox, но domain API не меняется.

### WF-10. REST actions и приёмка

Каждая action имеет отдельный request DTO, `@Valid`, expectedVersion, ролевой тест. Не делать `PATCH {status:"DONE"}`.

Приёмочный integration scenario:

1. Teacher creates.
2. Foreign head denied.
3. Own head approves.
4. Staff assigns officer.
5. Other officer denied.
6. Assigned officer starts.
7. Complete blocked by checklist.
8. Items completed.
9. Officer completes.
10. History has initial + every transition in order; notifications created after commit.
