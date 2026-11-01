# Модуль Security

## Зона ответственности

Модуль аутентифицирует пользователя, формирует `GrantedAuthority`, защищает HTTP endpoint и предоставляет
остальным модулям безопасный доступ к текущему локальному `userId`.

## Что уже есть

- BCrypt `PasswordEncoder`;
- `DatabaseUserDetailsService`, загружающий login, password hash и роль из БД;
- form login и logout;
- требование аутентификации для всех endpoint, кроме `/login`, `/error` и статики.

## Что нужно исправить

1. Нет `@EnableMethodSecurity`, поэтому `@PreAuthorize` не будет работать.
2. Нет компонента `CurrentUser`, из-за чего сервисы могут начать доверять `userId` из request body.
3. Стандартная HTML-форма входа не удобна для JSON API. Нужно либо зафиксировать session/form-login
   как контракт, либо отдельно спроектировать token-based API. Текущий гайд исходит из session/form-login.
4. CSRF включён по умолчанию. Его нельзя просто отключать для session API; frontend должен передавать токен.
5. В БД нет ни одного начального пользователя. Нужен одноразовый bootstrap-механизм или dev-миграция.

## План реализации

1. Ввести `AuthenticatedUser`, содержащий database ID, login, role и department ID.
2. Вернуть этот principal из `UserDetailsService`.
3. Добавить `CurrentUser`, чтобы бизнес-сервисы не зависели от `SecurityContextHolder`.
4. Включить method security и защитить методы сервисов, а не только URL.
5. Настроить JSON-ответы для `401` и `403`.
6. Настроить `CookieCsrfTokenRepository` для browser frontend.
7. Добавить endpoint `/api/v1/auth/me`.

## Пример principal

```java
public record AuthenticatedUser(
        Long id,
        String username,
        String password,
        String role,
        Long departmentId
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

Перед добавлением блокировки учётных записей метод `isEnabled()` возвращает `true`. После миграции он должен
возвращать значение из БД. Не включать password в логи и ручно написанный `toString`.

## Пример CurrentUser

```java
public interface CurrentUser {

    Long id();

    String role();

    Optional<Long> departmentId();
}
```

```java
@Component
class SecurityCurrentUser implements CurrentUser {

    private AuthenticatedUser principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authentication is required");
        }
        return user;
    }

    public Long id() {
        return principal().id();
    }

    public String role() {
        return principal().role();
    }

    public Optional<Long> departmentId() {
        return Optional.ofNullable(principal().departmentId());
    }
}
```

## Пример SecurityConfig

```java
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.csrfTokenRepository(
                        CookieCsrfTokenRepository.withHttpOnlyFalse()
                ))
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpStatus.NO_CONTENT.value())))
                .build();
    }
}
```

`HttpOnlyFalse` нужен, если browser JavaScript читает CSRF cookie и копирует её в header. Такой токен не является
токеном аутентификации.

## Тесты

- верный login/password создаёт сессию;
- неверный пароль не раскрывает, существует ли login;
- гость получает `401` на API;
- авторизованный пользователь без роли получает `403`;
- POST/PATCH/DELETE без CSRF-токена отклоняются;
- `CurrentUser.id()` возвращает database ID, а не login;
- роли в Spring Security имеют префикс `ROLE_` ровно один раз.

## Детальная декомпозиция задач

### SEC-01. Реализовать AuthenticatedUser

Зависит от: USR-01, USR-02.

Шаги:

1. Создать immutable principal в security-пакете.
2. Хранить database `id`, login, hash, role, department ID, enabled.
3. Не хранить JPA entity в principal: security session может жить дольше persistence context.
4. В `getAuthorities` добавлять ровно `ROLE_` + DB role.
5. Не печатать password/hash в `toString`, exception и audit log.

Готово, если principal сериализуем для session, не тянет Hibernate proxy, а unit-тест проверяет authority.

### SEC-02. Обновить DatabaseUserDetailsService

Шаги:

1. Искать login case-insensitive тем же способом, которым проверяется unique.
2. Загрузить role без N+1: `@EntityGraph(attributePaths = "role")` на repository method или transaction.
3. Смаппить `UserEntity` в `AuthenticatedUser` внутри transaction.
4. Все случаи «login не найден»/«disabled» отдавать наружу с одинаковым сообщением.

Тесты: existing, missing, disabled, role mapping, department null.

### SEC-03. Реализовать CurrentUser

Цель: изолировать domain services от Spring Security API.

Шаги:

1. Создать interface с `id`, `role`, `departmentId`.
2. Создать production implementation на `SecurityContextHolder`.
3. Для anonymous/missing principal бросать одно определённое exception.
4. Для unit-тестов сервисов создавать stub/mock `CurrentUser`, не поднимать SecurityContext.

Готово, если ни один domain service не принимает acting user ID из HTTP DTO.

### SEC-04. Включить method security

Шаги:

1. Добавить `@EnableMethodSecurity`.
2. Нанести `@PreAuthorize` на методы service/facade, где важна роль.
3. Проверки ownership/department оставить в Java domain service или в отдельном authorization bean.
4. Не полагаться только на URL rules: service может быть вызван event listener/job.
5. Помнить о self-invocation: `@PreAuthorize` не сработает при вызове метода того же bean через `this`.

### SEC-05. Настроить browser session и CSRF

Команда должна зафиксировать:

- успешный login: `204` или JSON, не HTML redirect;
- ошибка login: `401` с `ApiError`;
- logout: `204`, session invalidated;
- cookie flags: HttpOnly для session, SameSite, Secure в production;
- CSRF cookie/header names;
- CORS origins для dev frontend, если frontend на другом origin.

Не использовать `csrf.disable()` как быстрое исправление frontend-ошибки.

### SEC-06. Создать auth API и error handlers

1. `GET /api/v1/auth/me` возвращает ID/login/role/department, не password.
2. `AuthenticationEntryPoint` пишет JSON `401`.
3. `AccessDeniedHandler` пишет JSON `403`.
4. Единый mapper errors не печатает stack trace/client-sensitive details.
5. Добавить security headers; не отключать frame/XSS/content-type protection без причины.

### SEC-07. Создать dev bootstrap без утечки в production

Вариант: `db/devdata` migration с placeholder BCrypt hash или `ApplicationRunner` под `@Profile("local")`.

Обязательно:

- не хранить production password в Git;
- bootstrap идемпотентен;
- docker-профиль не создаёт admin с общеизвестным паролем;
- README описывает dev login и способ его смены.

### SEC-08. Матрица security-тестов

Для каждого защищённого controller минимум:

- anonymous → `401`;
- authenticated wrong role → `403`;
- correct role, wrong ownership/department → `403` или `404` по выбранной политике;
- correct role/scope → success;
- mutating request without CSRF → `403`;
- mutating request with CSRF → reaches controller.
