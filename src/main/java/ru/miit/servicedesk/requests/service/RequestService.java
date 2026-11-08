package ru.miit.servicedesk.requests.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.entity.RequestEntity;


public interface RequestService {

    RequestEntity create(Long userId, Long audienceId, Long serviceId, String description);

    RequestDetailsDto getById(Long id);

    Page<RequestSummaryDto> listByAuthor(Long userId, Pageable pageable);

    Page<RequestSummaryDto> listByExecutor(Long executorId, Pageable pageable);

}
