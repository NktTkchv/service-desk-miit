package ru.miit.servicedesk.requests;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestStatusRepository extends JpaRepository<RequestStatusEntity, Short> {

    Optional<RequestStatusEntity> findByName(String name);
}
