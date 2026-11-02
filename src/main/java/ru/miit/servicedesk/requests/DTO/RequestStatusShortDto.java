package ru.miit.servicedesk.requests.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// DTO для краткого обозначения статуса заявки
public record RequestStatusShortDto(
    @NotNull(message = "ID статуса не может быть пустым")
    Short id,
    @NotBlank(message = "Название статуса не может быть пустым или состоять из пробелов")
    String name

) {}
