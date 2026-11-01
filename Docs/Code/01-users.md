# Users: полный код

Сущности `UserEntity`, `RoleEntity`, `DepartmentEntity` уже есть. Ниже — код вокруг них. `AppUserRepository` после переноса удаляется.

## `users/RoleName.java`

```java
package ru.miit.servicedesk.users;

public enum RoleName {
    TEACHER,
    DEPARTMENT_HEAD,
    DEPARTMENT_STAFF,
    DUTY_OFFICER,
    ADMIN;

    public static RoleName fromDatabase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role name cannot be null");
        }
        return RoleName.valueOf(value);
    }
}
```

## `users/UserRepository.java`

```java
package ru.miit.servicedesk.users;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = {"role", "department"})
    Optional<UserEntity> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);

    @EntityGraph(attributePaths = {"role", "department"})
    Page<UserEntity> findByDepartment_Id(Long departmentId, Pageable pageable);
}
```

## DTO

### `users/dto/CreateUserRequest.java`

```java
package ru.miit.servicedesk.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

### `users/dto/UpdateUserRequest.java`

```java
package ru.miit.servicedesk.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String patronymic,
        @NotNull Short roleId,
        Long departmentId
) {
}
```

### `users/dto/ChangePasswordRequest.java`

```java
package ru.miit.servicedesk.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
```

### `users/dto/UserSummaryDto.java`

```java
package ru.miit.servicedesk.users.dto;

public record UserSummaryDto(Long id, String displayName) {
}
```

### `users/dto/UserDto.java`

```java
package ru.miit.servicedesk.users.dto;

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

## `users/UserMapper.java`

```java
package ru.miit.servicedesk.users;

import java.util.stream.Stream;

import ru.miit.servicedesk.users.dto.UserDto;
import ru.miit.servicedesk.users.dto.UserSummaryDto;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDto toDto(UserEntity user) {
        DepartmentEntity department = user.getDepartment();
        return new UserDto(
                user.getId(),
                user.getLogin(),
                user.getName(),
                user.getLastName(),
                user.getPatronymic(),
                user.getRole().getId(),
                user.getRole().getNameRole(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName()
        );
    }

    public static UserSummaryDto toSummary(UserEntity user) {
        String displayName = Stream.of(user.getLastName(), user.getName(), user.getPatronymic())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(user.getLogin());
        return new UserSummaryDto(user.getId(), displayName);
    }
}
```

## `users/UserService.java`

```java
package ru.miit.servicedesk.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.miit.servicedesk.users.dto.ChangePasswordRequest;
import ru.miit.servicedesk.users.dto.CreateUserRequest;
import ru.miit.servicedesk.users.dto.UpdateUserRequest;
import ru.miit.servicedesk.users.dto.UserDto;

public interface UserService {
    UserDto create(CreateUserRequest request);
    UserDto getById(Long id);
    Page<UserDto> findByDepartment(Long departmentId, Pageable pageable);
    UserDto update(Long id, UpdateUserRequest request);
    void changePassword(Long id, ChangePasswordRequest request);
}
```

## `users/UserServiceImpl.java`

```java
package ru.miit.servicedesk.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.miit.servicedesk.common.ConflictException;
import ru.miit.servicedesk.common.NotFoundException;
import ru.miit.servicedesk.users.dto.ChangePasswordRequest;
import ru.miit.servicedesk.users.dto.CreateUserRequest;
import ru.miit.servicedesk.users.dto.UpdateUserRequest;
import ru.miit.servicedesk.users.dto.UserDto;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDto create(CreateUserRequest request) {
        String login = request.login().trim();
        if (userRepository.existsByLoginIgnoreCase(login)) {
            throw new ConflictException("LOGIN_ALREADY_EXISTS", "Login is already occupied");
        }
        UserEntity user = new UserEntity();
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        apply(user, request.name(), request.lastName(), request.patronymic(),
                request.roleId(), request.departmentId());
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto getById(Long id) {
        return UserMapper.toDto(getRequired(id));
    }

    @Override
    public Page<UserDto> findByDepartment(Long departmentId, Pageable pageable) {
        return userRepository.findByDepartment_Id(departmentId, pageable).map(UserMapper::toDto);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        UserEntity user = getRequired(id);
        apply(user, request.name(), request.lastName(), request.patronymic(),
                request.roleId(), request.departmentId());
        return UserMapper.toDto(user);
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        getRequired(id).setPasswordHash(passwordEncoder.encode(request.password()));
    }

    private void apply(
            UserEntity user,
            String name,
            String lastName,
            String patronymic,
            Short roleId,
            Long departmentId
    ) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        DepartmentEntity department = departmentId == null ? null
                : departmentRepository.findById(departmentId)
                        .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
        user.setName(name.trim());
        user.setLastName(lastName.trim());
        user.setPatronymic(patronymic == null ? null : patronymic.trim());
        user.setRole(role);
        user.setDepartment(department);
    }

    private UserEntity getRequired(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
```

## `users/UserController.java`

```java
package ru.miit.servicedesk.users;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.miit.servicedesk.common.PageResponse;
import ru.miit.servicedesk.users.dto.ChangePasswordRequest;
import ru.miit.servicedesk.users.dto.CreateUserRequest;
import ru.miit.servicedesk.users.dto.UpdateUserRequest;
import ru.miit.servicedesk.users.dto.UserDto;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    UserDto getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping
    PageResponse<UserDto> findByDepartment(
            @RequestParam Long departmentId,
            Pageable pageable
    ) {
        return PageResponse.from(userService.findByDepartment(departmentId, pageable), value -> value);
    }

    @PatchMapping("/{id}")
    UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/password")
    ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
```

Для полного search по login/ФИО/role нужно добавить `JpaSpecificationExecutor` и `UserFilter`; приведённый controller показывает минимальный MVP.
