package ru.miit.servicedesk.requests.RequestMapper;
import ru.miit.servicedesk.catalog.CatalogMapper.CatalogMapper;
import ru.miit.servicedesk.requests.DTO.AudienceShortDto;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestStatusShortDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.entity.RequestStatusEntity;
import ru.miit.servicedesk.users.UserMapper.UserMapper;

public final class RequestMapper {

    private RequestMapper() {}

    public static AudienceShortDto toAudienceShort(AudienceEntity a) {
        return new AudienceShortDto(a.getId(), a.getNumber());
    }

    public static RequestStatusShortDto toStatusShort(RequestStatusEntity s) {
        return new RequestStatusShortDto(s.getId(), s.getName());
    }

    public static RequestSummaryDto toSummary(RequestEntity r) {
        return new RequestSummaryDto(
            r.getId(),
            r.getAudience().getNumber(),
            r.getService().getNameService(),
            r.getStatus().getName(),
            r.getDescription(),
            r.getCreatedAt());
    }

    public static RequestDetailsDto toDetails(RequestEntity r) {
        return new RequestDetailsDto(
            r.getId(),
            UserMapper.toShort(r.getAuthor()),
            toAudienceShort(r.getAudience()),
            CatalogMapper.toDto(r.getService()),
            toStatusShort(r.getStatus()),
            r.getDescription(),
            UserMapper.toShort(r.getApprover()),
            UserMapper.toShort(r.getExecutor()),
            r.getCreatedAt(),
            r.getApproved(),
            r.getRejectReason());
    }
}
