package ru.miit.servicedesk.requests;

import java.util.List;

public interface RequestService {

    RequestEntity create(Long userId, Long audienceId, Long serviceId, String description);

    RequestEntity getById(Long id);

    List<RequestEntity> listByAuthor(Long userId);

    void assign(Long requestId, Long executorId);

    void changeStatus(Long requestId, Short statusId, Long changedBy);
}
