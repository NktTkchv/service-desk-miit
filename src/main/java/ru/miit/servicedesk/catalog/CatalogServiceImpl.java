package ru.miit.servicedesk.catalog;


import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final ServiceCatalogItemRepository repository;

    public CatalogServiceImpl(ServiceCatalogItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceCatalogEntity> listAll() {
        return repository.findAll(Sort.by("nameService"));
    }

    @Override
    public ServiceCatalogEntity getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Услуга не найдена"));
    }

    @Override
    @Transactional
    public ServiceCatalogEntity create(String nameService, String description, Integer deadline) {
        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setNameService(nameService);
        entity.setDescription(description);
        entity.setDeadline(deadline);
        return repository.save(entity);
    }
}
