package ru.miit.servicedesk.requests.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.repository.ServiceCatalogRepository;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.RequestStatus;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.entity.RequestStatusEntity;
import ru.miit.servicedesk.requests.repository.AudienceRepository;
import ru.miit.servicedesk.requests.repository.RequestRepository;
import ru.miit.servicedesk.requests.repository.RequestStatusRepository;
import ru.miit.servicedesk.users.RoleEntity;
import ru.miit.servicedesk.users.UserEntity;
import ru.miit.servicedesk.users.repository.AppUserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {
    @Mock private RequestRepository requestRepository;
    @Mock private RequestStatusRepository statusRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private AudienceRepository audienceRepository;
    @Mock private ServiceCatalogRepository catalogRepository;
    private RequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RequestServiceImpl(requestRepository, statusRepository, userRepository,
            audienceRepository, catalogRepository);
    }

    @Test
    void createResolvesReferencesSetsDeadlineAndSavesRequest() {
        UserEntity author = user();
        AudienceEntity audience = audience();
        ServiceCatalogEntity catalog = catalog();
        RequestStatusEntity status = status();
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(audienceRepository.findById(2L)).thenReturn(Optional.of(audience));
        when(catalogRepository.findById(3L)).thenReturn(Optional.of(catalog));
        when(statusRepository.findByName("PENDING_HEAD")).thenReturn(Optional.of(status));
        when(requestRepository.save(any(RequestEntity.class))).thenAnswer(invocation -> {
            RequestEntity request = invocation.getArgument(0);
            request.setId(50L);
            return request;
        });
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        RequestEntity result = service.create(1L, 2L, 3L, "description");

        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);
        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getAuthor()).isSameAs(author);
        assertThat(result.getAudience()).isSameAs(audience);
        assertThat(result.getService()).isSameAs(catalog);
        assertThat(result.getStatus()).isSameAs(status);
        assertThat(result.getDescription()).isEqualTo("description");
        assertThat(result.getDeadlineHoursSnapshot()).isEqualTo(24);
        assertThat(result.getDueAt()).isBetween(before.plusHours(24), after.plusHours(24));
        verify(statusRepository).findByName(RequestStatus.PENDING_HEAD.name());
        verify(requestRepository).save(result);
    }

    @Test
    void createFailsWhenAuthorIsMissingAndStopsLookupChain() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, 2L, 3L, "d"))
            .withMessage("Пользователь не найден");
        verifyNoInteractions(audienceRepository, catalogRepository, statusRepository, requestRepository);
    }

    @Test
    void createFailsWhenAudienceIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(audienceRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, 2L, 3L, "d"))
            .withMessage("Аудитория не найдена");
        verifyNoInteractions(catalogRepository, statusRepository, requestRepository);
    }

    @Test
    void createFailsWhenCatalogServiceIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(audienceRepository.findById(2L)).thenReturn(Optional.of(audience()));
        when(catalogRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, 2L, 3L, "d"))
            .withMessage("Услуга не найдена");
        verifyNoInteractions(statusRepository, requestRepository);
    }

    @Test
    void createFailsWhenInitialStatusIsNotConfigured() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(audienceRepository.findById(2L)).thenReturn(Optional.of(audience()));
        when(catalogRepository.findById(3L)).thenReturn(Optional.of(catalog()));
        when(statusRepository.findByName("PENDING_HEAD")).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(1L, 2L, 3L, "d"))
            .withMessage("Ошибка статус не настроен");
        verifyNoInteractions(requestRepository);
    }

    @Test
    void getByIdMapsFoundRequestToDetails() {
        RequestEntity request = completeRequest();
        when(requestRepository.findWithDetailsById(9L)).thenReturn(Optional.of(request));

        RequestDetailsDto result = service.getById(9L);

        assertThat(result.id()).isEqualTo(9L);
        assertThat(result.audience().number()).isEqualTo("Б-202");
        assertThat(result.service().nameService()).isEqualTo("Сеть");
    }

    @Test
    void getByIdFailsWhenRequestIsMissing() {
        when(requestRepository.findWithDetailsById(404L)).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.getById(404L))
            .withMessage("Заявка не найдена");
    }

    @Test
    void listByAuthorMapsPageAndPreservesPageMetadata() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<RequestEntity> source = new PageImpl<>(List.of(completeRequest()), pageable, 8);
        when(requestRepository.findByAuthor_Id(1L, pageable)).thenReturn(source);

        Page<RequestSummaryDto> result = service.listByAuthor(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(source.getTotalElements());
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().extracting(RequestSummaryDto::id).isEqualTo(9L);
    }

    @Test
    void listByExecutorMapsRepositoryPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findByExecutor_Id(7L, pageable))
            .thenReturn(new PageImpl<>(List.of(completeRequest()), pageable, 1));

        Page<RequestSummaryDto> result = service.listByExecutor(7L, pageable);

        assertThat(result.getContent()).singleElement()
            .extracting(RequestSummaryDto::statusName).isEqualTo("PENDING_HEAD");
    }

    private static UserEntity user() {
        RoleEntity role = new RoleEntity();
        role.setNameRole("ROLE_USER");
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setLastName("Иванов");
        user.setName("Иван");
        user.setPatronymic("Иванович");
        user.setRole(role);
        return user;
    }

    private static AudienceEntity audience() {
        AudienceEntity entity = new AudienceEntity();
        entity.setId(2L);
        entity.setNumber("Б-202");
        return entity;
    }

    private static ServiceCatalogEntity catalog() {
        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setId(3L);
        entity.setNameService("Сеть");
        entity.setDescription("Доступ к сети");
        entity.setDeadline(24);
        return entity;
    }

    private static RequestStatusEntity status() {
        RequestStatusEntity status = new RequestStatusEntity();
        status.setId((short) 1);
        status.setName("PENDING_HEAD");
        return status;
    }

    private static RequestEntity completeRequest() {
        RequestEntity request = new RequestEntity();
        request.setId(9L);
        request.setAuthor(user());
        request.setAudience(audience());
        request.setService(catalog());
        request.setStatus(status());
        request.setDescription("Нет сети");
        request.setCreatedAt(OffsetDateTime.of(2026, 2, 3, 4, 5, 0, 0, ZoneOffset.UTC));
        request.setApproved(false);
        return request;
    }
}
