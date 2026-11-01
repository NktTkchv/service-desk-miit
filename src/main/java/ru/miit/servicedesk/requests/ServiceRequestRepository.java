package ru.miit.servicedesk.requests;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<RequestEntity, Long> {
}
