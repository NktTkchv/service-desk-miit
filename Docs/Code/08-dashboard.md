# Dashboard: полный MVP-код

```java
package ru.miit.servicedesk.dashboard;
public interface StatusCountProjection { String getStatus(); long getCount(); }
```

```java
package ru.miit.servicedesk.dashboard;
public interface ExecutorLoadProjection { Long getExecutorId(); String getExecutorName(); long getCount(); }
```

```java
package ru.miit.servicedesk.dashboard;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.miit.servicedesk.requests.entity.RequestEntity;
public interface DashboardRepository extends JpaRepository<RequestEntity, Long> {
    @Query("""
            select s.name as status, count(r.id) as count
            from RequestEntity r join r.status s
            where r.createdAt >= :from and r.createdAt < :to
              and (:departmentId is null or r.author.department.id = :departmentId)
            group by s.name
            """)
    List<StatusCountProjection> countByStatus(@Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to, @Param("departmentId") Long departmentId);

    @Query("""
            select e.id as executorId,
                   concat(e.lastName, ' ', e.name) as executorName,
                   count(r.id) as count
            from RequestEntity r join r.executor e join r.status s
            where s.name in ('ASSIGNED', 'IN_PROGRESS')
              and (:departmentId is null or r.author.department.id = :departmentId)
            group by e.id, e.lastName, e.name order by count(r.id) desc
            """)
    List<ExecutorLoadProjection> loadByExecutor(@Param("departmentId") Long departmentId);
}
```

```java
package ru.miit.servicedesk.dashboard;
import java.util.*;
public record ExecutorLoadDto(Long executorId, String executorName, long activeRequests) {}
public record DashboardDto(long open, Map<String, Long> byStatus, List<ExecutorLoadDto> byExecutor) {}
```

Каждый public record вынести в отдельный `.java`.

```java
package ru.miit.servicedesk.dashboard;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.miit.servicedesk.security.CurrentUser;
@Service @Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    private static final Set<String> TERMINAL = Set.of("DONE", "REJECTED", "CANCELLED");
    private final DashboardRepository repository; private final CurrentUser currentUser;
    public DashboardServiceImpl(DashboardRepository r, CurrentUser c) { repository = r; currentUser = c; }
    public DashboardDto get(LocalDate from, LocalDate to) {
        Long department = switch (currentUser.role()) {
            case "ADMIN" -> null;
            case "DEPARTMENT_HEAD", "DEPARTMENT_STAFF" -> currentUser.departmentId().orElseThrow();
            default -> throw new AccessDeniedException("Dashboard is not available");
        };
        OffsetDateTime start = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Map<String, Long> counts = new LinkedHashMap<>();
        repository.countByStatus(start, end, department).forEach(p -> counts.put(p.getStatus(), p.getCount()));
        long open = counts.entrySet().stream().filter(e -> !TERMINAL.contains(e.getKey()))
                .mapToLong(Map.Entry::getValue).sum();
        List<ExecutorLoadDto> load = repository.loadByExecutor(department).stream()
                .map(p -> new ExecutorLoadDto(p.getExecutorId(), p.getExecutorName(), p.getCount())).toList();
        return new DashboardDto(open, counts, load);
    }
}
```

`DashboardService` заменить на `DashboardDto get(LocalDate from, LocalDate to)`. Controller:

```java
package ru.miit.servicedesk.dashboard;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardServiceImpl service;
    public DashboardController(DashboardServiceImpl s) { service = s; }
    @GetMapping DashboardDto get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("from must not be after to");
        return service.get(from, to);
    }
}
```

Для полного контракта добавить unassigned/overdue/service filter и mapper `IllegalArgumentException` в `400`, как описано в основном гайде.
