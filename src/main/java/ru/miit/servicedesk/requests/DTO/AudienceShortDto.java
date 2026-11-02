package ru.miit.servicedesk.requests.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// DTO для обозначения аудитории в RequestDetailsDto
public record AudienceShortDto(

    @NotNull(message = "ID аудитории не может быть пустым")
    Long id,

    @NotBlank(message = "Номер аудитории не может быть пустым или состоять из пробелов")
    String number

) {}
