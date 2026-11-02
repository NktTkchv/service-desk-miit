package ru.miit.servicedesk.users.UserMapper;

import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.UserEntity;

public class UserMapper {
    public static UserSummaryDto toShort(UserEntity u) {
        if (u == null) return null;
        return new UserSummaryDto(u.getId(),
            u.getLastName() + " " + u.getName() + " " + u.getPatronymic(),
            u.getRole().getNameRole());
    }
}
