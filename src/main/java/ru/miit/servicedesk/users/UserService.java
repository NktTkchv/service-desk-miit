package ru.miit.servicedesk.users;

import java.util.List;

public interface UserService {

    UserEntity findById(Long id);

    UserEntity findByLogin(String login);

    List<UserEntity> listByDepartment(Long departmentId);

    UserEntity create(
            String login,
            String rawPassword,
            String name,
            String lastName,
            String patronymic,
            Short roleId,
            Long departmentId
    );
}
