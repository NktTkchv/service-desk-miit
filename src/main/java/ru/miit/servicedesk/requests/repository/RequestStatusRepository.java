package ru.miit.servicedesk.requests.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.miit.servicedesk.requests.entity.RequestStatusEntity;

@Repository
public interface RequestStatusRepository extends JpaRepository<RequestStatusEntity, Short> {

    Optional<RequestStatusEntity> findByName(String name);
}
