package ru.miit.servicedesk.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.repository.ServiceCatalogRepository;

import java.util.List;

@Service
public class CatalogServiceImpl implements CatalogService {
    ServiceCatalogRepository serviceCatalogRepository;

    public CatalogServiceImpl(ServiceCatalogRepository serviceCatalogRepository) {
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Override
    @Transactional
    public ServiceCatalogEntity create(String nameService, String description, Integer deadline) {
        String name = nameService == null ? "" : nameService.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("nameService must not be blank");
        }
        if (deadline == null || deadline <= 0) {
            throw new IllegalArgumentException("deadline must be > 0");
        }
        if (serviceCatalogRepository.existsByNameServiceIgnoreCase(name)) {
            throw new IllegalArgumentException("Service name already exists: " + name);
        }

        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setNameService(name);
        entity.setDescription(description);
        entity.setDeadline(deadline);
        return serviceCatalogRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogEntity> listAll() {
        return serviceCatalogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceCatalogEntity getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return serviceCatalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    }
}
