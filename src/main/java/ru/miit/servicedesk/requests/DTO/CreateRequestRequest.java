package ru.miit.servicedesk.requests.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRequestRequest(
    @NotNull Long audienceId,
    @NotNull Long serviceId,
    @Size(max = 4000) String description
) {
}
