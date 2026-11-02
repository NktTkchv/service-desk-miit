package ru.miit.servicedesk.catalog.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Dto для обозначения услуги в RequestDetailsDto
public record ServiceCatalogDto(

    @NotNull(message = "ID услуги не может быть пустым")
    Long id,

    @NotBlank(message = "Название услуги не может быть пустым")
    String nameService,

    // Описание может быть необязательным (например, null, если его нет в каталоге)
    String description,

    @NotNull(message = "Срок выполнения (deadline) обязателен")
    @Positive(message = "Срок выполнения должен быть положительным числом (в днях или часах)")
    Integer deadline

) {}
