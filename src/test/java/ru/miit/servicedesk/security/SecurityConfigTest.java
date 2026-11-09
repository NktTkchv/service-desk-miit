package ru.miit.servicedesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    @Test
    void passwordEncoderUsesBcryptAndMatchesEncodedPassword() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        String encoded = encoder.encode("secret");
        assertThat(encoded).startsWith("$2");
        assertThat(encoder.matches("secret", encoded)).isTrue();
        assertThat(encoder.matches("wrong", encoded)).isFalse();
    }
}
