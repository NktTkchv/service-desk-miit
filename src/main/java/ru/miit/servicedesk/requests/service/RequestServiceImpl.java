package ru.miit.servicedesk.requests.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.RequestMapper.RequestMapper;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.repository.RequestRepository;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.requests.repository.RequestStatusRepository;
import ru.miit.servicedesk.users.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService{
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final AppUserRepository user;


    public RequestServiceImpl(RequestRepository requestRepository, RequestStatusRepository requestStatusRepository, AppUserRepository user) {
        this.requestRepository = requestRepository;
        this.requestStatusRepository = requestStatusRepository;
        this.user = user;
    }

    @Override
    @Transactional
    public RequestEntity create(Long userId, Long audienceId, Long serviceId, String description) {
        return null;
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
