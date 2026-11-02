package ru.miit.servicedesk.requests.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.miit.servicedesk.requests.entity.AudienceEntity;

@Repository
public interface AudienceRepository extends JpaRepository<AudienceEntity, Long> {
}
