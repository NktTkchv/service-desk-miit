package ru.miit.servicedesk.requests.DTO;
import java.time.OffsetDateTime;

public record RequestSummaryDto(
    Long id,
    String audienceNum,
    String serviceName,
    String statusName,
    String description,
    OffsetDateTime createdAt
) { }
