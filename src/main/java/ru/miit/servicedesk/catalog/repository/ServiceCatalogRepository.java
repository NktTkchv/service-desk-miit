package ru.miit.servicedesk.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogEntity, Long> {
    boolean existsByNameServiceIgnoreCase(String nameService);
}
