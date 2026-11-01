package ru.miit.servicedesk.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogItemRepository extends JpaRepository<ServiceCatalogEntity, Long> {
}
