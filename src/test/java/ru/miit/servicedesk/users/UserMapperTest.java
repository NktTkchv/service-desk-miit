package ru.miit.servicedesk.users;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.UserMapper.UserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserMapperTest {
    @Test
    void toShortReturnsNullForNullUser() { assertThat(UserMapper.toShort(null)).isNull(); }

    @Test
    void toShortMapsFullNameAndRole() {
        assertThat(UserMapper.toShort(user()))
            .isEqualTo(new UserSummaryDto(4L, "Иванов Иван Иванович", "ROLE_USER"));
    }

    @Test
    void toUserDetailsMapsCredentialsAndRole() {
        UserDetails result = UserMapper.toUserDetails(user());
        assertThat(result.getUsername()).isEqualTo("ivanov");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void toUserDetailsRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> UserMapper.toUserDetails(null))
            .withMessage("User in mapper cannot be null");
    }

    private static UserEntity user() {
        RoleEntity role = new RoleEntity();
        role.setNameRole("ROLE_USER");
        UserEntity user = new UserEntity();
        user.setId(4L);
        user.setLogin("ivanov");
        user.setPasswordHash("hash");
        user.setLastName("Иванов");
        user.setName("Иван");
        user.setPatronymic("Иванович");
        user.setRole(role);
        return user;
    }
}
