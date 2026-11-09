package ru.miit.servicedesk.users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.miit.servicedesk.users.UserEntity;

public interface AppUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    List<UserEntity> findByDepartment_Id(Long departmentId);
}
