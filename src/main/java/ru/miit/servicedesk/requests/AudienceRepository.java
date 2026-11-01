package ru.miit.servicedesk.requests;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AudienceRepository extends JpaRepository<AudienceEntity, Long> {
}
