# Эталонная реализация

В этом каталоге находятся полные листинги целевого MVP. Это учебный reference, а не сигнал скопировать всё одним
commit. Каждый блок нужно переносить в рамках задачи из модульного гайда.

## Порядок переноса

1. [Common API](00-common.md).
2. [Users](01-users.md).
3. [Security](02-security.md).
4. [Catalog](03-catalog.md).
5. [Requests](04-requests.md).
6. [Workflow](05-workflow.md).
7. [Checklists](06-checklists.md).
8. [Notifications](07-notifications.md).
9. [Dashboard](08-dashboard.md).
10. [Infrastructure and tests](09-infrastructure.md).

## Важные оговорки

- Номера Flyway нужно выбирать по фактическому состоянию main. В листингах используются смысловые имена.
- Код описывает session/form-login MVP, а не JWT.
- Спорные domain-решения зафиксированы как рекомендуемый MVP. Если команда выбирает иначе, сначала
  обновляются module guide и REST API.
- После каждого шага запускается `./gradlew clean test`.
