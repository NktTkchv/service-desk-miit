package ru.miit.servicedesk.security.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultUserDetailsTest {
    @Test
    void exposesConstructorValuesAndDefaultAccountFlags() {
        DefaultUserDetails details = new DefaultUserDetails("login", "hash", "ROLE_ADMIN");
        assertThat(details.getUsername()).isEqualTo("login");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }
}
