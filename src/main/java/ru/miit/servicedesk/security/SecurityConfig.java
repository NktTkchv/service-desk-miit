package ru.miit.servicedesk.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
//
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        return http
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers("/login", "/error", "/css/**", "/js/**")
//                        .permitAll()
//                        .anyRequest()
//                        .authenticated())
//                .formLogin(form -> form.permitAll())
//                .logout(logout -> logout.permitAll())
//                .build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Отключаем CSRF для тестов
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // <--- ПУСКАЕМ ВСЕХ ВЕЗДЕ
            )
            .formLogin(form -> form.disable()); // Отключаем страницу логина
        return http.build();
    }
}
