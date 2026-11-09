package ru.miit.servicedesk.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.miit.servicedesk.catalog.DTO.ServiceCatalogDto;
import ru.miit.servicedesk.requests.DTO.*;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() { factory.close(); }

    @Test
    void serviceCatalogDtoAcceptsValidValue() {
        assertThat(validator.validate(new ServiceCatalogDto(1L, "Сеть", null, 24))).isEmpty();
    }

    @Test
    void serviceCatalogDtoRejectsMissingIdBlankNameAndNonPositiveDeadline() {
        Set<ConstraintViolation<ServiceCatalogDto>> violations =
            validator.validate(new ServiceCatalogDto(null, "  ", null, 0));

        assertThat(paths(violations)).containsExactlyInAnyOrder("id", "nameService", "deadline");
    }

    @Test
    void audienceShortDtoValidatesIdAndNumber() {
        assertThat(paths(validator.validate(new AudienceShortDto(null, "  "))))
            .containsExactlyInAnyOrder("id", "number");
    }

    @Test
    void requestStatusShortDtoValidatesIdAndName() {
        assertThat(paths(validator.validate(new RequestStatusShortDto(null, ""))))
            .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void userSummaryDtoRequiresOnlyId() {
        assertThat(paths(validator.validate(new UserSummaryDto(null, null, null))))
            .containsExactly("id");
    }

    @Test
    void createRequestFormAcceptsNullDescriptionAndValidIds() {
        CreateRequestForm form = new CreateRequestForm();
        form.setAuthorId(1L);
        form.setAudienceId(2L);
        form.setServiceId(3L);

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void createRequestFormRejectsMissingIdsAndTooLongDescription() {
        CreateRequestForm form = new CreateRequestForm();
        form.setDescription("x".repeat(4001));

        assertThat(paths(validator.validate(form)))
            .containsExactlyInAnyOrder("authorId", "audienceId", "serviceId", "description");
    }

    @Test
    void createRequestRequestValidatesRequiredIdsAndDescriptionLength() {
        CreateRequestRequest request = new CreateRequestRequest(null, null, "x".repeat(4001));

        assertThat(paths(validator.validate(request)))
            .containsExactlyInAnyOrder("audienceId", "serviceId", "description");
    }

    @Test
    void requestDetailsDtoAcceptsValidRequiredFieldsAndNullableAssignments() {
        RequestDetailsDto dto = validDetails();

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void requestDetailsDtoRejectsAllMissingRequiredFields() {
        RequestDetailsDto dto = new RequestDetailsDto(
            null, null, null, null, null, " ", null, null, null, null, null);

        assertThat(paths(validator.validate(dto))).containsExactlyInAnyOrder(
            "id", "author", "audience", "service", "status", "description", "createdAt", "approved");
    }

    private static RequestDetailsDto validDetails() {
        return new RequestDetailsDto(
            1L,
            new UserSummaryDto(2L, "Иванов Иван Иванович", "ROLE_USER"),
            new AudienceShortDto(3L, "А-101"),
            new ServiceCatalogDto(4L, "Сеть", null, 24),
            new RequestStatusShortDto((short) 1, "PENDING_HEAD"),
            "Нет сети",
            null,
            null,
            OffsetDateTime.now(),
            false,
            null);
    }

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
    }
}
