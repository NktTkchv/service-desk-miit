package ru.miit.servicedesk.catalog.service;

import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;

import java.util.List;

public interface CatalogService {

    List<ServiceCatalogEntity> listAll();

    ServiceCatalogEntity getById(Long id);

    ServiceCatalogEntity create(String nameService, String description, Integer deadline);
}
