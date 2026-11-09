package ru.miit.servicedesk.catalog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.repository.ServiceCatalogRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {
    @Mock private ServiceCatalogRepository repository;
    private CatalogServiceImpl service;

    @BeforeEach
    void setUp() { service = new CatalogServiceImpl(repository); }

    @Test
    void createTrimsNameBuildsEntityAndReturnsSavedValue() {
        when(repository.existsByNameServiceIgnoreCase("Телефония")).thenReturn(false);
        when(repository.save(any(ServiceCatalogEntity.class))).thenAnswer(invocation -> {
            ServiceCatalogEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        ServiceCatalogEntity result = service.create("  Телефония  ", "IP-телефон", 8);

        ArgumentCaptor<ServiceCatalogEntity> captor = ArgumentCaptor.forClass(ServiceCatalogEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getNameService()).isEqualTo("Телефония");
        assertThat(captor.getValue().getDescription()).isEqualTo("IP-телефон");
        assertThat(captor.getValue().getDeadline()).isEqualTo(8);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void createRejectsNullNameWithoutCallingRepository() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(null, null, 1))
            .withMessage("nameService must not be blank");
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsBlankNameWithoutCallingRepository() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create("   ", null, 1))
            .withMessage("nameService must not be blank");
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsNullDeadline() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create("Услуга", null, null))
            .withMessage("deadline must be > 0");
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsNonPositiveDeadline() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create("Услуга", null, 0))
            .withMessage("deadline must be > 0");
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsDuplicateNameAfterTrimming() {
        when(repository.existsByNameServiceIgnoreCase("Услуга")).thenReturn(true);
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(" Услуга ", null, 5))
            .withMessage("Service name already exists: Услуга");
        verify(repository, never()).save(any());
    }

    @Test
    void listAllReturnsRepositoryResult() {
        List<ServiceCatalogEntity> entities = List.of(new ServiceCatalogEntity(), new ServiceCatalogEntity());
        when(repository.findAll()).thenReturn(entities);
        assertThat(service.listAll()).isSameAs(entities);
    }

    @Test
    void getByIdReturnsFoundEntity() {
        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        when(repository.findById(3L)).thenReturn(Optional.of(entity));
        assertThat(service.getById(3L)).isSameAs(entity);
    }

    @Test
    void getByIdRejectsNullWithoutCallingRepository() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.getById(null))
            .withMessage("id must not be null");
        verifyNoInteractions(repository);
    }

    @Test
    void getByIdRejectsMissingService() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.getById(99L))
            .withMessage("Service not found: 99");
    }
}
