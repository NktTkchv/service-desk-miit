package ru.miit.servicedesk.requests.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.RequestMapper.RequestMapper;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.entity.RequestStatusEntity;
import ru.miit.servicedesk.requests.repository.RequestRepository;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.requests.repository.RequestStatusRepository;
import ru.miit.servicedesk.users.AppUserRepository;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService{
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final AppUserRepository user;


    //По документу 0.4md в какой статус может перейти заявка, нужно для метода принятия заявки
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        "PENDING_HEAD",       Set.of("PENDING_DEPARTMENT", "REJECTED"),
        "PENDING_DEPARTMENT", Set.of("ASSIGNED", "REJECTED"),
        "ASSIGNED",           Set.of("IN_PROGRESS", "CANCELLED"),
        "IN_PROGRESS",        Set.of("DONE", "CANCELLED"));

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

    @Override
    @Transactional
    public void assign(Long requestId, Long executorId) {
        //Ищу заявку
        RequestEntity requestEntity = requestRepository.findById(requestId).
            orElseThrow(() -> new NoSuchElementException("Заявка с данным айди не найдена"));
        if(!"PENDING_DEPARTMENT".equals(requestEntity.getStatus().getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Назначить исполнителя можно только на этапе PENDING_DEPARTMENT");
        }

        //назначаю исполнителя
        requestEntity.setExecutor(user.findById(executorId)
            .orElseThrow(() -> new NoSuchElementException("Исполнитель с таким ID не найден")));
        //Меняю статус
        requestEntity.setStatus(requestStatusRepository.findByName("ASSIGNED").
            orElseThrow(() -> new IllegalStateException("Статус " + "ASSIGNED" + " не настроен")));
    }

    @Override
    @Transactional
    public void changeStatus(Long requestId, Short statusId, Long changedBy) {
        RequestEntity requestEntity = requestRepository.findById(requestId).
            orElseThrow(() -> new NoSuchElementException("Заявка с данным айди не найдена"));
        RequestStatusEntity requestStatusEntity = requestStatusRepository.findById(statusId)
            .orElseThrow(() -> new NoSuchElementException("Статус не найден"));

        //Либо статус либо пустой список
        Set<String> allowed = TRANSITIONS.getOrDefault(requestEntity.getStatus().getName(), Set.of());
        //Если в допустимом списке переходов нет значения, переход невозможен
        if (!allowed.contains(requestStatusEntity.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Переход " + requestEntity.getStatus().getName() + " → " + requestStatusEntity.getName() + " запрещён");

        }
        requestEntity.setStatus(requestStatusEntity);
    }
}
