# Catalog: полный код

## Migration

```sql
ALTER TABLE service_catalog
    ADD COLUMN active boolean NOT NULL DEFAULT true;
```

В `ServiceCatalogEntity` добавить:

```java
@Column(name = "active", nullable = false)
private boolean active = true;

public boolean isActive() { return active; }
public void setActive(boolean active) { this.active = active; }
```

## `catalog/ServiceCatalogRepository.java`

```java
package ru.miit.servicedesk.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogEntity, Long> {
    boolean existsByNameServiceIgnoreCase(String name);
    boolean existsByNameServiceIgnoreCaseAndIdNot(String name, Long id);
    Optional<ServiceCatalogEntity> findByIdAndActiveTrue(Long id);
    Page<ServiceCatalogEntity> findByNameServiceContainingIgnoreCaseAndActiveTrue(
            String query, Pageable pageable);
    Page<ServiceCatalogEntity> findByNameServiceContainingIgnoreCase(
            String query, Pageable pageable);
}
```

## DTO

```java
package ru.miit.servicedesk.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CatalogItemRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        @NotNull @Positive Integer deadlineHours
) {
}
```

```java
package ru.miit.servicedesk.catalog.dto;

public record CatalogItemDto(
        Long id, String name, String description, Integer deadlineHours, boolean active
) {
}
```

## `catalog/CatalogMapper.java`

```java
package ru.miit.servicedesk.catalog;

import ru.miit.servicedesk.catalog.dto.CatalogItemDto;

public final class CatalogMapper {
    private CatalogMapper() {
    }

    public static CatalogItemDto toDto(ServiceCatalogEntity entity) {
        return new CatalogItemDto(
                entity.getId(), entity.getNameService(), entity.getDescription(),
                entity.getDeadline(), entity.isActive()
        );
    }
}
```

## `catalog/CatalogService.java`

```java
package ru.miit.servicedesk.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.catalog.dto.CatalogItemDto;
import ru.miit.servicedesk.catalog.dto.CatalogItemRequest;

public interface CatalogService {
    Page<CatalogItemDto> find(String query, boolean includeInactive, Pageable pageable);
    CatalogItemDto getById(Long id, boolean includeInactive);
    CatalogItemDto create(CatalogItemRequest request);
    CatalogItemDto update(Long id, CatalogItemRequest request);
    void deactivate(Long id);
}
```

## `catalog/CatalogServiceImpl.java`

```java
package ru.miit.servicedesk.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.miit.servicedesk.catalog.dto.CatalogItemDto;
import ru.miit.servicedesk.catalog.dto.CatalogItemRequest;
import ru.miit.servicedesk.common.ConflictException;
import ru.miit.servicedesk.common.NotFoundException;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {
    private final ServiceCatalogRepository repository;

    public CatalogServiceImpl(ServiceCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<CatalogItemDto> find(String query, boolean includeInactive, Pageable pageable) {
        String normalized = query == null ? "" : query.trim();
        Page<ServiceCatalogEntity> page = includeInactive
                ? repository.findByNameServiceContainingIgnoreCase(normalized, pageable)
                : repository.findByNameServiceContainingIgnoreCaseAndActiveTrue(normalized, pageable);
        return page.map(CatalogMapper::toDto);
    }

    @Override
    public CatalogItemDto getById(Long id, boolean includeInactive) {
        ServiceCatalogEntity entity = includeInactive
                ? repository.findById(id).orElseThrow(() -> notFound(id))
                : repository.findByIdAndActiveTrue(id).orElseThrow(() -> notFound(id));
        return CatalogMapper.toDto(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogItemDto create(CatalogItemRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameServiceIgnoreCase(name)) {
            throw duplicate();
        }
        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        apply(entity, request, name);
        entity.setActive(true);
        return CatalogMapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogItemDto update(Long id, CatalogItemRequest request) {
        ServiceCatalogEntity entity = repository.findById(id).orElseThrow(() -> notFound(id));
        String name = request.name().trim();
        if (repository.existsByNameServiceIgnoreCaseAndIdNot(name, id)) {
            throw duplicate();
        }
        apply(entity, request, name);
        return CatalogMapper.toDto(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(Long id) {
        repository.findById(id).orElseThrow(() -> notFound(id)).setActive(false);
    }

    private void apply(ServiceCatalogEntity entity, CatalogItemRequest request, String name) {
        entity.setNameService(name);
        entity.setDescription(request.description());
        entity.setDeadline(request.deadlineHours());
    }

    private NotFoundException notFound(Long id) {
        return new NotFoundException("Catalog item not found: " + id);
    }

    private ConflictException duplicate() {
        return new ConflictException("SERVICE_NAME_EXISTS", "Service name already exists");
    }
}
```

## `catalog/CatalogController.java`

```java
package ru.miit.servicedesk.catalog;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.miit.servicedesk.catalog.dto.CatalogItemDto;
import ru.miit.servicedesk.catalog.dto.CatalogItemRequest;
import ru.miit.servicedesk.common.PageResponse;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping
    PageResponse<CatalogItemDto> find(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable
    ) {
        return PageResponse.from(service.find(query, includeInactive, pageable), value -> value);
    }

    @GetMapping("/{id}")
    CatalogItemDto get(@PathVariable Long id) { return service.getById(id, false); }

    @PostMapping
    ResponseEntity<CatalogItemDto> create(@Valid @RequestBody CatalogItemRequest request) {
        CatalogItemDto dto = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/catalog/" + dto.id())).body(dto);
    }

    @PatchMapping("/{id}")
    CatalogItemDto update(@PathVariable Long id, @Valid @RequestBody CatalogItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
```

`includeInactive` должен быть доступен только ADMIN. В production controller нужно либо проверять роль и принудительно сбрасывать флаг, либо вынести admin-list в `/api/v1/admin/catalog`.
