package ru.miit.servicedesk.catalog;

import java.util.List;

public interface CatalogService {

    List<ServiceCatalogEntity> listAll();

    ServiceCatalogEntity getById(Long id);

    ServiceCatalogEntity create(String nameService, String description, Integer deadline);
}
