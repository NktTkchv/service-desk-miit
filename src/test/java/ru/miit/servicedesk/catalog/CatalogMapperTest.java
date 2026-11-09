package ru.miit.servicedesk.catalog;

import org.junit.jupiter.api.Test;
import ru.miit.servicedesk.catalog.CatalogMapper.CatalogMapper;
import ru.miit.servicedesk.catalog.DTO.ServiceCatalogDto;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogMapperTest {
    @Test
    void toDtoMapsEveryField() {
        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setId(7L);
        entity.setNameService("Ремонт проектора");
        entity.setDescription("Замена лампы");
        entity.setDeadline(24);

        ServiceCatalogDto result = CatalogMapper.toDto(entity);

        assertThat(result).isEqualTo(new ServiceCatalogDto(7L, "Ремонт проектора", "Замена лампы", 24));
    }
}
