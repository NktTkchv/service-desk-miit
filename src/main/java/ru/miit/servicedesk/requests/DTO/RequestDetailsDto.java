package ru.miit.servicedesk.requests.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.miit.servicedesk.catalog.DTO.ServiceCatalogDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;

import java.time.OffsetDateTime;

// DTO с полной информацией о заявке для карточки (Details view)
public record RequestDetailsDto(

    @NotNull(message = "ID заявки не может быть пустым")
    Long id,

    @NotNull(message = "У заявки обязательно должен быть автор")
    UserSummaryDto author,

    @NotNull(message = "Аудитория должна быть указана")
    AudienceShortDto audience,

    @NotNull(message = "Услуга должна быть указана")
    ServiceCatalogDto service,

    @NotNull(message = "Статус заявки обязателен")
    RequestStatusShortDto status,

    @NotBlank(message = "Описание заявки не может быть пустым")
    String description,

    // Согласователь может быть еще не назначен (null)
    UserSummaryDto approver,

    // Исполнитель может быть еще не назначен (null)
    UserSummaryDto executor,

    @NotNull(message = "Дата создания обязательна")
    OffsetDateTime createdAt,

    @NotNull(message = "Флаг согласования не может быть null (должен быть true или false)")
    Boolean approved,

    // Причина отклонения будет null, если заявку еще не отклонили
    String rejectReason

) {}
