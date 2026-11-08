package ru.miit.servicedesk.catalog.CatalogMapper;

import ru.miit.servicedesk.catalog.DTO.ServiceCatalogDto;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;

public class CatalogMapper {
    public static ServiceCatalogDto toDto(ServiceCatalogEntity s) {
        return new ServiceCatalogDto(s.getId(), s.getNameService(), s.getDescription(), s.getDeadline());
    }
}
