# Модуль Users

## Зона ответственности

Модуль хранит пользователей, роли и подразделения. Он отвечает за создание учётных записей,
хеширование паролей, поиск пользователей и привязку к роли и подразделению.

## Что уже есть

- `UserEntity`, `RoleEntity`, `DepartmentEntity` и JPA-связи между ними;
- `AppUserRepository`, `RoleRepository`, `DepartmentRepository`;
- интерфейс `UserService`;
- роли, заполняемые Flyway: `TEACHER`, `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF`, `DUTY_OFFICER`, `ADMIN`.

## Что нужно исправить перед реализацией

1. `AppUserRepository` назван по старому имени сущности. Переименовать в `UserRepository`.
2. `UserService` возвращает JPA-сущности. Для границы модуля лучше ввести `UserDto` и `CreateUserCommand`.
3. В таблице `users` нет признака блокировки. Если нужно отключать учётные записи, добавить `enabled`
   отдельной Flyway-миграцией.
4. В схеме нет прямой связи `department.head_user_id`. Руководитель сейчас может определяться как пользователь
   с ролью `DEPARTMENT_HEAD` в том же подразделении. Нужно решить, допустимы ли несколько руководителей.

## Целевые DTO

```java
public record CreateUserRequest(
        @NotBlank @Size(max = 100) String login,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String patronymic,
        @NotNull Short roleId,
        Long departmentId
) {
}
```

```java
public record UserDto(
        Long id,
        String login,
        String name,
        String lastName,
        String patronymic,
        Short roleId,
        String role,
        Long departmentId,
        String department
) {
}
```

`passwordHash` не должен попадать ни в один response DTO.

## План реализации

1. Переименовать репозиторий и добавить `existsByLoginIgnoreCase`.
2. Ввести enum `RoleName`, чтобы не разносить строковые константы по коду.
3. Создать DTO и mapper. MapStruct для такого объёма не обязателен; достаточно явного mapper-класса.
4. Реализовать `UserServiceImpl` с `@Transactional`.
5. В `create` проверить уникальность login, наличие роли и отдела, захешировать пароль.
6. Добавить admin REST-контроллер и защитить его `@PreAuthorize("hasRole('ADMIN')")`.
7. Добавить получение своего профиля без передачи `userId` клиентом.

## Пример репозитория

```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);

    Page<UserEntity> findByDepartment_Id(Long departmentId, Pageable pageable);
}
```

## Пример создания пользователя

```java
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByLoginIgnoreCase(request.login())) {
            throw new ConflictException("LOGIN_ALREADY_EXISTS", "Login is already occupied");
        }

        RoleEntity role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        DepartmentEntity department = request.departmentId() == null
                ? null
                : departmentRepository.findById(request.departmentId())
                        .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        UserEntity user = new UserEntity();
        user.setLogin(request.login().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        user.setLastName(request.lastName().trim());
        user.setPatronymic(request.patronymic());
        user.setRole(role);
        user.setDepartment(department);

        return UserMapper.toDto(userRepository.save(user));
    }
}
```

## Правила доступа

| Операция | Кто может выполнить |
| --- | --- |
| Получить свой профиль | Любой авторизованный пользователь |
| Просматривать всех пользователей | `ADMIN` |
| Создавать и менять учётные записи | `ADMIN` |
| Просматривать сотрудников своего отдела | `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF` |

## Тесты

- пароль хешируется и не совпадает с исходной строкой;
- дублирующийся login возвращает `409 Conflict`;
- неизвестные role/department возвращают `404 Not Found`;
- пароль никогда не возвращается в JSON;
- не-`ADMIN` получает `403 Forbidden` на admin endpoint;
- поиск по department не возвращает людей из другого отдела.

## Детальная декомпозиция задач

### USR-01. Привести имена и контракты в порядок

Цель: убрать старые имена и не дать JPA entities стать публичным API.

Шаги:

1. Переименовать `AppUserRepository.java` и интерфейс в `UserRepository`.
2. Обновить injection в `DatabaseUserDetailsService` и будущих сервисах.
3. Создать пакеты `users.dto`, `users.internal` и `users.web` или другую единую структуру.
4. Изменить `UserService`: на входе command/ID, на выходе DTO, никаких `UserEntity` в public-методах.

Проверка: `rg 'AppUserRepository|UserEntity find' src/main/java` не должен находить public service contract.

Готово, если: проект собирается, security использует `UserRepository`, entity не попадает в controller.

### USR-02. Зафиксировать роли

Зависит от: USR-01.

Шаги:

1. Создать enum `RoleName` с пятью значениями из `V1__init.sql`.
2. Не менять `RoleEntity.nameRole` на `@Enumerated` без решения команды: справочник role уже хранится в отдельной таблице.
3. Добавить `RoleName fromDatabase(String value)`, чтобы неизвестная роль давала ясную ошибку.
4. Во всём бизнес-коде сравнивать enum, а не строки.

Тесты: все seed-значения парсятся; unknown/null обрабатываются предсказуемо.

### USR-03. Создать DTO и mapper

Файлы: `CreateUserRequest`, `UpdateUserRequest`, `ChangePasswordRequest`, `UserDto`, `UserSummaryDto`, `UserMapper`.

Шаги:

1. Наложить Validation согласно длинам колонок БД.
2. Не принимать `passwordHash` от клиента, только raw `password` в create/change-password command.
3. `UserSummaryDto` ограничить `id` и display name; использовать в request DTO.
4. Mapper не должен обращаться к LAZY-коллекциям `authoredRequests`/`approvedRequests`/`assignedRequests`.
5. Обсудить формат display name и написать один метод, а не склеивать ФИО в каждом controller.

Готово, если: в JSON нет password hash и bidirectional collections; mapper покрыт unit-тестом.

### USR-04. Реализовать UserService

Зависит от: USR-01–USR-03, bean `PasswordEncoder`.

Методы:

- `create(CreateUserRequest)`;
- `getById(long)`;
- `find(UserFilter, Pageable)`;
- `update(long, UpdateUserRequest)`;
- `changePassword(long, ChangePasswordRequest)`;
- позже `setEnabled(long, boolean)` после migration.

Для `create`: trim login/name, проверить unique login, загрузить role/department, проверить domain-правила, BCrypt encode, save, map.

Для `update`: не менять password; не вызывать `save` без необходимости для managed entity; проверить, что последний admin не
лишается роли, если это требование команды.

Транзакции: класс `@Transactional(readOnly = true)`, create/update/change password — `@Transactional`.

### USR-05. Создать REST controller

Шаги:

1. `GET /api/v1/users` с фильтрами `query`, `role`, `departmentId`, `page`, `size`, `sort`.
2. `POST /api/v1/users` — `@Valid`, `201`, `Location`.
3. `GET/PATCH /api/v1/users/{id}` — `404`, если ID нет.
4. `PUT /api/v1/users/{id}/password` — admin reset или self-change; для self-change требовать current password.
5. На методах admin API поставить `@PreAuthorize`.

Не писать бизнес-логику, repository calls и password encoding в controller.

### USR-06. Решить модель руководителя отдела

Нерешённый вопрос, блокирующий workflow:

- вариант A: руководитель — любой user с `DEPARTMENT_HEAD` и department; может быть несколько;
- вариант B: в department добавляется один `head_user_id`, нужна migration и circular FK;
- вариант C: отдельная history-таблица назначений.

Для учебного MVP проще вариант A. Команда должна записать решение в этом гайде и добавить DB constraint/application validation.

### USR-07. Покрыть модуль тестами

- unit: service с Mockito, все branch create/update;
- `@DataJpaTest`: case-insensitive login, department query, pagination;
- `@WebMvcTest`: validation, roles, status codes, response без hash;
- integration: create → login через Spring Security;
- migration: чистая PostgreSQL поднимается с ролями.
