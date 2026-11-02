package ru.miit.servicedesk.requests.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.entity.RequestEntity;

import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<RequestEntity, Long> {
    // JOIN FETCH для связей: подгрузит их одним запросом, без N+1
    @EntityGraph(attributePaths = {"author", "audience", "service", "status", "approver", "executor"})
    Optional<RequestEntity> findWithDetailsById(Long id);

    // "Author_Id" = поле author -> его поле id; Pageable даёт страницу и сортировку
    @EntityGraph(attributePaths = {"author", "audience", "service", "status"})
    Page<RequestEntity> findByAuthor_Id(Long authorId, Pageable pageable);

    Page<RequestEntity> findByExecutor_Id(Long executorId, Pageable pageable);
}
