# Service Desk

Учебный Service Desk для работы с заявками, каталогом услуг и процессом согласования.

Стек: Java 21, Spring Boot 3.5.7, Gradle, PostgreSQL 18, Redis 7, Flyway, Spring Data JPA,
Spring Security Form Login, Docker и Docker Compose.

> Проект учебный и намеренно не завершён. Часть сервисов описана только интерфейсами.
> Это не ошибка сборки: недостающие реализации команда должна добавить самостоятельно.

Подробный план развития по модулям, примеры кода и целевой REST API находятся в [Docs](Docs/README.md).

## Профили Spring Boot

В проекте есть два профиля:

| Профиль | Для чего | PostgreSQL | Redis |
| --- | --- | --- | --- |
| `local` | Запуск из IDE, Gradle или JAR на компьютере | `localhost:5432` | `localhost:6379` |
| `docker` | Запуск приложения в Docker Compose | `postgres:5432` | `redis:6379` |

`local` — профиль по умолчанию. Если профиль не указан, Spring Boot активирует именно его.

Compose автоматически передаёт контейнеру приложения `SPRING_PROFILES_ACTIVE=docker`.
Вручную включать `docker` при запуске с хоста обычно не нужно: имена `postgres` и `redis` доступны
именно внутри Docker-сети.

## Быстрый запуск в Docker

Это основной сценарий запуска всего проекта.

### Требования

- Docker;
- Docker Compose v2 — команда `docker compose`.

Локально устанавливать Java и Gradle для этого сценария не нужно: они есть на build-этапе `Dockerfile`.

### 1. Подготовьте настройки

```bash
cp .env.example .env
```

В `.env.example` заданы учебные значения:

```dotenv
DB_NAME=servicedesk
DB_USER=servicedesk
DB_PASSWORD=servicedesk
```

Вне локальной учебной среды обязательно замените пароль.

### 2. Соберите и запустите проект

Из корневого каталога выполните:

```bash
docker compose up -d --build app
```

Команда:

1. соберёт Spring Boot JAR внутри Docker;
2. создаст образ `app`;
3. запустит PostgreSQL и Redis;
4. дождётся их готовности через healthcheck;
5. запустит приложение с профилем `docker`;
6. применит Flyway-миграции.

После запуска:

- приложение: <http://localhost:8080>;
- форма Spring Security: <http://localhost:8080/login>;
- PostgreSQL: `localhost:5432`;
- Redis: `localhost:6379`.

### 3. Проверьте запуск

```bash
docker compose ps
docker compose logs -f app
```

Выход из просмотра логов — `Ctrl+C`. Контейнеры при этом продолжат работу.

После изменения исходного кода пересоберите образ той же командой:

```bash
docker compose up -d --build app
```

### 4. Остановите проект

```bash
docker compose down
```

Эта команда сохраняет том `pgdata` с данными PostgreSQL. Для полного сброса базы:

```bash
docker compose down -v
```

> Внимание: `docker compose down -v` безвозвратно удаляет все локальные данные PostgreSQL.

## Локальный запуск с профилем `local`

Этот сценарий удобен для разработки: PostgreSQL и Redis работают в Docker, Spring Boot — на компьютере.

Потребуются Java 21, Docker и Docker Compose. Отдельно устанавливать Gradle не нужно.

### 1. Запустите инфраструктуру

```bash
cp .env.example .env
docker compose up -d postgres redis
```

### 2. Запустите Spring Boot

macOS/Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows:

```bat
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Так как `local` — профиль по умолчанию, можно запустить короче:

```bash
./gradlew bootRun
```

В IDE запустите `ServiceDeskApplication.main()`. Профиль можно задать через:

- program arguments: `--spring.profiles.active=local`;
- environment variable: `SPRING_PROFILES_ACTIVE=local`;
- либо не задавать его и использовать `local` по умолчанию.

### Запуск собранного JAR

```bash
./gradlew clean bootJar
java -jar build/libs/service-desk-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Запуск с профилем `docker`

Штатный способ запуска этого профиля:

```bash
docker compose up -d --build app
```

Профиль включается переменной `SPRING_PROFILES_ACTIVE=docker`, заданной в `docker-compose.yml`.

Для отладочного запуска профиля `docker` с хоста нужно переопределить Docker-имена на `localhost`:

```bash
docker compose up -d postgres redis
DB_HOST=localhost REDIS_HOST=localhost \
  ./gradlew bootRun --args='--spring.profiles.active=docker'
```

## Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | Активный Spring-профиль |
| `DB_HOST` | `localhost` / `postgres` | Хост PostgreSQL, зависит от профиля |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `servicedesk` | Имя базы |
| `DB_USER` | `servicedesk` | Пользователь PostgreSQL |
| `DB_PASSWORD` | `servicedesk` | Пароль PostgreSQL |
| `REDIS_HOST` | `localhost` / `redis` | Хост Redis, зависит от профиля |
| `REDIS_PORT` | `6379` | Порт Redis |

## База данных и Flyway

`V1__init.sql` создаёт `users`, `role`, `department`, `audience`, `service_catalog`, `request` и `request_status`.
После миграции Hibernate проверяет схему в режиме `ddl-auto: validate`.

Не изменяйте уже применённую `V1__init.sql` для базы с важными данными. Изменения схемы оформляйте
новыми Flyway-миграциями, например `V2__add_checklist.sql`.

## Авторизация

`DatabaseUserDetailsService` получает пользователя и его роль из PostgreSQL. В новой базе нет тестовых
учётных записей. Пароли в `users.password_hash` должны храниться только в виде BCrypt-хеша.

Регистрацию, администрирование пользователей и тестовые данные команде ещё предстоит реализовать.

## Что ещё нужно реализовать

- сервисы работы с заявками, каталогом и пользователями;
- `workflow`, `checklists`, `dashboard` и `notifications`;
- MVC/REST-контроллеры;
- HTML-шаблоны и CSRF-aware формы;
- разграничение доступа по ролям;
- управление учётными записями;
- бизнес-правила согласования;
- автотесты.

Проверка сборки:

```bash
./gradlew clean test
```
