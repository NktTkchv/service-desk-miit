package ru.miit.servicedesk.users.UserMapper;

import org.springframework.security.core.userdetails.UserDetails;
import ru.miit.servicedesk.security.dto.DefaultUserDetails;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.UserEntity;

public class UserMapper {
    public static UserSummaryDto toShort(UserEntity u) {
        if (u == null) return null;
        return new UserSummaryDto(u.getId(),
            u.getLastName() + " " + u.getName() + " " + u.getPatronymic(),
            u.getRole().getNameRole());
    }

    public static UserDetails toUserDetails(UserEntity user) {
        if (user == null) {
            throw new NullPointerException("User in mapper cannot be null");
        }
        return new DefaultUserDetails(
            user.getLogin(),
            user.getPasswordHash(),
            user.getRole().getNameRole()
        );
    }
}
