package ru.miit.servicedesk.requests;

import org.junit.jupiter.api.Test;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.RequestMapper.RequestMapper;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.entity.RequestStatusEntity;
import ru.miit.servicedesk.users.RoleEntity;
import ru.miit.servicedesk.users.UserEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMapperTest {

    @Test
    void toAudienceShortMapsFields() {
        AudienceEntity audience = new AudienceEntity();
        audience.setId(2L);
        audience.setNumber("А-101");

        assertThat(RequestMapper.toAudienceShort(audience))
            .extracting("id", "number")
            .containsExactly(2L, "А-101");
    }

    @Test
    void toStatusShortMapsFields() {
        RequestStatusEntity status = new RequestStatusEntity();
        status.setId((short) 3);
        status.setName("IN_PROGRESS");

        assertThat(RequestMapper.toStatusShort(status))
            .extracting("id", "name")
            .containsExactly((short) 3, "IN_PROGRESS");
    }

    @Test
    void toSummaryMapsFlattenedRequestFields() {
        RequestEntity request = request();

        RequestSummaryDto result = RequestMapper.toSummary(request);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.audienceNum()).isEqualTo("А-101");
        assertThat(result.serviceName()).isEqualTo("Доступ к Wi-Fi");
        assertThat(result.statusName()).isEqualTo("PENDING_HEAD");
        assertThat(result.description()).isEqualTo("Нет сигнала");
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.of(2026, 1, 2, 3, 4, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void toDetailsMapsNestedObjectsAndNullableUsers() {
        RequestEntity request = request();
        request.setApprover(null);
        request.setExecutor(user(12L, "Петров", "Петр", "Петрович", "ROLE_EXECUTOR"));

        RequestDetailsDto result = RequestMapper.toDetails(request);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.author().fullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(result.audience().number()).isEqualTo("А-101");
        assertThat(result.service().nameService()).isEqualTo("Доступ к Wi-Fi");
        assertThat(result.service().deadline()).isEqualTo(12);
        assertThat(result.status().name()).isEqualTo("PENDING_HEAD");
        assertThat(result.approver()).isNull();
        assertThat(result.executor().id()).isEqualTo(12L);
        assertThat(result.approved()).isFalse();
        assertThat(result.rejectReason()).isEqualTo("нет");
    }

    private static RequestEntity request() {
        AudienceEntity audience = new AudienceEntity();
        audience.setId(5L);
        audience.setNumber("А-101");
        ServiceCatalogEntity service = new ServiceCatalogEntity();
        service.setId(6L);
        service.setNameService("Доступ к Wi-Fi");
        service.setDescription("Сетевой доступ");
        service.setDeadline(12);
        RequestStatusEntity status = new RequestStatusEntity();
        status.setId((short) 1);
        status.setName("PENDING_HEAD");
        RequestEntity request = new RequestEntity();
        request.setId(42L);
        request.setAuthor(user(9L, "Иванов", "Иван", "Иванович", "ROLE_USER"));
        request.setAudience(audience);
        request.setService(service);
        request.setStatus(status);
        request.setDescription("Нет сигнала");
        request.setCreatedAt(OffsetDateTime.of(2026, 1, 2, 3, 4, 0, 0, ZoneOffset.UTC));
        request.setApproved(false);
        request.setRejectReason("нет");
        return request;
    }

    private static UserEntity user(Long id, String lastName, String name, String patronymic, String roleName) {
        RoleEntity role = new RoleEntity();
        role.setNameRole(roleName);
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setLastName(lastName);
        user.setName(name);
        user.setPatronymic(patronymic);
        user.setRole(role);
        return user;
    }
}
