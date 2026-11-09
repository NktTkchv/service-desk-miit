package ru.miit.servicedesk.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.miit.servicedesk.users.RoleEntity;
import ru.miit.servicedesk.users.UserEntity;
import ru.miit.servicedesk.users.repository.AppUserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {
    @Mock private AppUserRepository repository;
    private DatabaseUserDetailsService service;

    @BeforeEach
    void setUp() { service = new DatabaseUserDetailsService(repository); }

    @Test
    void loadUserByUsernameReturnsMappedUser() {
        RoleEntity role = new RoleEntity();
        role.setNameRole("ROLE_DISPATCHER");
        UserEntity user = new UserEntity();
        user.setLogin("dispatcher");
        user.setPasswordHash("encoded");
        user.setRole(role);
        when(repository.findByLogin("dispatcher")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("dispatcher");

        assertThat(result.getUsername()).isEqualTo("dispatcher");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_DISPATCHER");
    }

    @Test
    void loadUserByUsernameThrowsSpringExceptionWhenUserIsMissing() {
        when(repository.findByLogin("missing")).thenReturn(Optional.empty());
        assertThatExceptionOfType(UsernameNotFoundException.class)
            .isThrownBy(() -> service.loadUserByUsername("missing"))
            .withMessage("User not found");
    }
}
