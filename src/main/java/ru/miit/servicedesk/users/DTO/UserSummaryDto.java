package ru.miit.servicedesk.users.DTO;
//Dto для обозначения пользователей в RequestDetailsDto

import jakarta.validation.constraints.NotNull;

public record UserSummaryDto(
    @NotNull Long id,
    String fullName,
    String roleName
) { }
