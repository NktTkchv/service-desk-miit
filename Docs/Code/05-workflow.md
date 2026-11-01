# Workflow: полный код

## `workflow/RequestStatusHistoryEntity.java`

```java
package ru.miit.servicedesk.workflow;

import java.time.OffsetDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.miit.servicedesk.requests.RequestEntity;
import ru.miit.servicedesk.requests.RequestStatusEntity;
import ru.miit.servicedesk.users.UserEntity;

@Entity
@Table(name = "request_status_history")
public class RequestStatusHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false) private RequestEntity request;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id") private RequestStatusEntity fromStatus;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_status_id", nullable = false) private RequestStatusEntity toStatus;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false) private UserEntity changedBy;
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false) private OffsetDateTime changedAt;
    @Column(name = "reason", columnDefinition = "text") private String reason;

    public Long getId() { return id; }
    public RequestEntity getRequest() { return request; }
    public void setRequest(RequestEntity request) { this.request = request; }
    public RequestStatusEntity getFromStatus() { return fromStatus; }
    public void setFromStatus(RequestStatusEntity value) { this.fromStatus = value; }
    public RequestStatusEntity getToStatus() { return toStatus; }
    public void setToStatus(RequestStatusEntity value) { this.toStatus = value; }
    public UserEntity getChangedBy() { return changedBy; }
    public void setChangedBy(UserEntity value) { this.changedBy = value; }
    public OffsetDateTime getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

## Repositories and commands

```java
package ru.miit.servicedesk.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestStatusHistoryRepository
        extends JpaRepository<RequestStatusHistoryEntity, Long> {
}
```

```java
package ru.miit.servicedesk.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VersionCommand(@NotNull Long expectedVersion) {
}
public record RejectCommand(
        @NotBlank @Size(max = 2000) String reason,
        @NotNull Long expectedVersion
) {
}
public record AssignCommand(@NotNull Long executorId, @NotNull Long expectedVersion) {
}
```

В Java каждый public record должен лежать в отдельном файле: `VersionCommand.java`, `RejectCommand.java`, `AssignCommand.java`.

## `workflow/RequestStatusChangedEvent.java`

```java
package ru.miit.servicedesk.workflow;

import java.time.Instant;
import java.util.UUID;

public record RequestStatusChangedEvent(
        UUID eventId, Long requestId, String previousStatus, String newStatus,
        Long actorId, Instant occurredAt
) {
}
```

## `workflow/RequestWorkflowService.java`

```java
package ru.miit.servicedesk.workflow;

import ru.miit.servicedesk.requests.dto.RequestDto;

public interface RequestWorkflowService {
    RequestDto approveByHead(Long id, long version);
    RequestDto rejectByHead(Long id, String reason, long version);
    RequestDto assign(Long id, Long executorId, long version);
    RequestDto rejectByDepartment(Long id, String reason, long version);
    RequestDto start(Long id, long version);
    RequestDto complete(Long id, long version);
    RequestDto cancel(Long id, String reason, long version);
}
```

## `workflow/RequestWorkflowServiceImpl.java`

```java
package ru.miit.servicedesk.workflow;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.miit.servicedesk.checklists.ChecklistService;
import ru.miit.servicedesk.common.BusinessRuleException;
import ru.miit.servicedesk.common.ConflictException;
import ru.miit.servicedesk.common.NotFoundException;
import ru.miit.servicedesk.requests.*;
import ru.miit.servicedesk.requests.dto.RequestDto;
import ru.miit.servicedesk.security.CurrentUser;
import ru.miit.servicedesk.users.*;

@Service
@Transactional
public class RequestWorkflowServiceImpl implements RequestWorkflowService {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            RequestStatus.PENDING_HEAD, Set.of(RequestStatus.PENDING_DEPARTMENT,
                    RequestStatus.REJECTED, RequestStatus.CANCELLED),
            RequestStatus.PENDING_DEPARTMENT, Set.of(RequestStatus.ASSIGNED,
                    RequestStatus.REJECTED, RequestStatus.CANCELLED),
            RequestStatus.ASSIGNED, Set.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED),
            RequestStatus.IN_PROGRESS, Set.of(RequestStatus.DONE, RequestStatus.CANCELLED),
            RequestStatus.DONE, Set.of(), RequestStatus.REJECTED, Set.of(),
            RequestStatus.CANCELLED, Set.of()
    );

    private final RequestRepository requests;
    private final RequestStatusRepository statuses;
    private final RequestStatusHistoryRepository history;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final ChecklistService checklists;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RequestWorkflowServiceImpl(
            RequestRepository requests, RequestStatusRepository statuses,
            RequestStatusHistoryRepository history, UserRepository users,
            CurrentUser currentUser, ChecklistService checklists,
            ApplicationEventPublisher events, Clock clock
    ) {
        this.requests = requests; this.statuses = statuses; this.history = history;
        this.users = users; this.currentUser = currentUser; this.checklists = checklists;
        this.events = events; this.clock = clock;
    }

    public RequestDto approveByHead(Long id, long version) {
        RequestEntity request = load(id, version);
        requireRole("DEPARTMENT_HEAD"); requireSameDepartment(request);
        requireStatus(request, RequestStatus.PENDING_HEAD);
        request.setApprover(actor());
        transition(request, RequestStatus.PENDING_DEPARTMENT, null);
        return RequestMapper.toDto(request);
    }

    public RequestDto rejectByHead(Long id, String reason, long version) {
        RequestEntity request = load(id, version);
        requireRole("DEPARTMENT_HEAD"); requireSameDepartment(request);
        requireStatus(request, RequestStatus.PENDING_HEAD);
        request.setApprover(actor()); request.setApproved(false); request.setRejectReason(reason);
        transition(request, RequestStatus.REJECTED, reason);
        return RequestMapper.toDto(request);
    }

    public RequestDto assign(Long id, Long executorId, long version) {
        RequestEntity request = load(id, version);
        requireRole("DEPARTMENT_STAFF"); requireSameDepartment(request);
        requireStatus(request, RequestStatus.PENDING_DEPARTMENT);
        UserEntity executor = users.findById(executorId)
                .orElseThrow(() -> new NotFoundException("Executor not found"));
        if (!"DUTY_OFFICER".equals(executor.getRole().getNameRole())) {
            throw new BusinessRuleException("INVALID_EXECUTOR", "Executor must be DUTY_OFFICER");
        }
        request.setExecutor(executor);
        transition(request, RequestStatus.ASSIGNED, null);
        return RequestMapper.toDto(request);
    }

    public RequestDto rejectByDepartment(Long id, String reason, long version) {
        RequestEntity request = load(id, version);
        requireRole("DEPARTMENT_STAFF"); requireSameDepartment(request);
        requireStatus(request, RequestStatus.PENDING_DEPARTMENT);
        request.setApproved(false); request.setRejectReason(reason);
        transition(request, RequestStatus.REJECTED, reason);
        return RequestMapper.toDto(request);
    }

    public RequestDto start(Long id, long version) {
        RequestEntity request = load(id, version);
        requireExecutor(request); requireStatus(request, RequestStatus.ASSIGNED);
        transition(request, RequestStatus.IN_PROGRESS, null);
        return RequestMapper.toDto(request);
    }

    public RequestDto complete(Long id, long version) {
        RequestEntity request = load(id, version);
        requireExecutor(request); requireStatus(request, RequestStatus.IN_PROGRESS);
        if (!checklists.areRequiredItemsComplete(id)) {
            throw new BusinessRuleException("CHECKLIST_INCOMPLETE", "Required checklist is incomplete");
        }
        transition(request, RequestStatus.DONE, null);
        return RequestMapper.toDto(request);
    }

    public RequestDto cancel(Long id, String reason, long version) {
        RequestEntity request = load(id, version);
        boolean author = request.getAuthor().getId().equals(currentUser.id());
        if (!(author || "ADMIN".equals(currentUser.role()))) throw new AccessDeniedException("Denied");
        transition(request, RequestStatus.CANCELLED, reason);
        return RequestMapper.toDto(request);
    }

    private RequestEntity load(Long id, long version) {
        RequestEntity request = requests.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (request.getVersion() != version) throw new ConflictException("STALE_VERSION", "Stale version");
        return request;
    }

    private void transition(RequestEntity request, RequestStatus target, String reason) {
        RequestStatus previous = RequestStatus.valueOf(request.getStatus().getName());
        if (!ALLOWED.getOrDefault(previous, Set.of()).contains(target)) {
            throw new ConflictException("INVALID_TRANSITION", previous + " -> " + target);
        }
        RequestStatusEntity targetEntity = statuses.findByName(target.name())
                .orElseThrow(() -> new IllegalStateException("Status not configured: " + target));
        RequestStatusHistoryEntity row = new RequestStatusHistoryEntity();
        row.setRequest(request); row.setFromStatus(request.getStatus()); row.setToStatus(targetEntity);
        row.setChangedBy(actor()); row.setReason(reason); history.save(row);
        request.setStatus(targetEntity);
        events.publishEvent(new RequestStatusChangedEvent(UUID.randomUUID(), request.getId(),
                previous.name(), target.name(), currentUser.id(), clock.instant()));
    }

    private UserEntity actor() { return users.getReferenceById(currentUser.id()); }
    private void requireRole(String role) {
        if (!role.equals(currentUser.role())) throw new AccessDeniedException("Role denied");
    }
    private void requireStatus(RequestEntity r, RequestStatus expected) {
        if (!expected.name().equals(r.getStatus().getName()))
            throw new ConflictException("INVALID_STATUS", "Expected " + expected);
    }
    private void requireSameDepartment(RequestEntity r) {
        Long actual = r.getAuthor().getDepartment() == null ? null : r.getAuthor().getDepartment().getId();
        Long expected = currentUser.departmentId()
                .orElseThrow(() -> new AccessDeniedException("Department is required"));
        if (!expected.equals(actual))
            throw new AccessDeniedException("Foreign department");
    }
    private void requireExecutor(RequestEntity r) {
        requireRole("DUTY_OFFICER");
        if (r.getExecutor() == null || !r.getExecutor().getId().equals(currentUser.id()))
            throw new AccessDeniedException("Not assigned executor");
    }
}
```

## Controller

```java
package ru.miit.servicedesk.workflow;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.miit.servicedesk.requests.dto.RequestDto;
import ru.miit.servicedesk.workflow.dto.*;

@RestController
@RequestMapping("/api/v1/requests/{id}/actions")
public class RequestWorkflowController {
    private final RequestWorkflowService service;
    public RequestWorkflowController(RequestWorkflowService service) { this.service = service; }

    @PostMapping("/approve-by-head") RequestDto approve(@PathVariable Long id,
            @Valid @RequestBody VersionCommand c) { return service.approveByHead(id, c.expectedVersion()); }
    @PostMapping("/reject-by-head") RequestDto rejectHead(@PathVariable Long id,
            @Valid @RequestBody RejectCommand c) { return service.rejectByHead(id, c.reason(), c.expectedVersion()); }
    @PostMapping("/assign") RequestDto assign(@PathVariable Long id,
            @Valid @RequestBody AssignCommand c) { return service.assign(id, c.executorId(), c.expectedVersion()); }
    @PostMapping("/reject-by-department") RequestDto rejectDepartment(@PathVariable Long id,
            @Valid @RequestBody RejectCommand c) { return service.rejectByDepartment(id, c.reason(), c.expectedVersion()); }
    @PostMapping("/start") RequestDto start(@PathVariable Long id,
            @Valid @RequestBody VersionCommand c) { return service.start(id, c.expectedVersion()); }
    @PostMapping("/complete") RequestDto complete(@PathVariable Long id,
            @Valid @RequestBody VersionCommand c) { return service.complete(id, c.expectedVersion()); }
    @PostMapping("/cancel") RequestDto cancel(@PathVariable Long id,
            @Valid @RequestBody RejectCommand c) { return service.cancel(id, c.reason(), c.expectedVersion()); }
}
```
