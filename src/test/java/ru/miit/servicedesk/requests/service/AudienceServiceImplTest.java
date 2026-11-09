package ru.miit.servicedesk.requests.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.repository.AudienceRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudienceServiceImplTest {
    @Mock private AudienceRepository repository;

    @Test
    void listAllReturnsRepositoryResult() {
        List<AudienceEntity> audiences = List.of(new AudienceEntity(), new AudienceEntity());
        when(repository.findAll()).thenReturn(audiences);

        assertThat(new AudienceServiceImpl(repository).listAll()).isSameAs(audiences);
    }
}
