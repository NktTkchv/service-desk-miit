package ru.miit.servicedesk.requests.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.repository.ServiceCatalogRepository;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.RequestMapper.RequestMapper;
import ru.miit.servicedesk.requests.RequestStatus;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.entity.RequestStatusEntity;
import ru.miit.servicedesk.requests.repository.AudienceRepository;
import ru.miit.servicedesk.requests.repository.RequestRepository;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.requests.repository.RequestStatusRepository;
import ru.miit.servicedesk.users.AppUserRepository;
import ru.miit.servicedesk.users.UserEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService{
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final AppUserRepository user;
    private final AudienceRepository audienceRepository;
    private final ServiceCatalogRepository catalogRepository;


    public RequestServiceImpl(RequestRepository requestRepository, RequestStatusRepository requestStatusRepository, AppUserRepository user, AudienceRepository audienceRepository, ServiceCatalogRepository catalogRepository) {
        this.requestRepository = requestRepository;
        this.requestStatusRepository = requestStatusRepository;
        this.user = user;
        this.audienceRepository = audienceRepository;
        this.catalogRepository = catalogRepository;
    }

    @Override
    @Transactional
    public RequestEntity create(Long userId, Long audienceId, Long serviceId, String description) {
        UserEntity author = user.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        AudienceEntity audience = audienceRepository.findById(audienceId)
            .orElseThrow(() -> new IllegalArgumentException("Аудитория не найдена"));
        ServiceCatalogEntity service = catalogRepository.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Услуга не найдена"));
        RequestStatusEntity status = requestStatusRepository
            .findByName(RequestStatus.PENDING_HEAD.name())
            .orElseThrow(() -> new IllegalArgumentException("Ошибка статус не настроен"));


        RequestEntity request = new RequestEntity();
        request.setAuthor(author);
        request.setAudience(audience);
        request.setService(service);
        request.setStatus(status);
        request.setDescription(description);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        request.setDeadlineHoursSnapshot(service.getDeadline());
        request.setDueAt(now.plusHours(service.getDeadline()));

        RequestEntity saved = requestRepository.save(request);

        return saved;
    }

    @Override
    public RequestDetailsDto getById(Long id) {
        return requestRepository.findWithDetailsById(id)
            .map(RequestMapper::toDetails)
            .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
    }


    @Override
    public Page<RequestSummaryDto> listByAuthor(Long userId, Pageable pageable) {
        return requestRepository.findByAuthor_Id(userId, pageable)
            .map(RequestMapper::toSummary);
    }

    @Override
    public Page<RequestSummaryDto> listByExecutor(Long executorId, Pageable pageable) {
        return requestRepository.findByExecutor_Id(executorId, pageable)
            .map(RequestMapper::toSummary);
    }

}
