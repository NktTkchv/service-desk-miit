# Infrastructure: код и конфигурация

## Gradle test dependencies

```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
```

## Clock/async configuration

```java
package ru.miit.servicedesk;
import java.time.Clock;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
@Configuration @EnableAsync
public class ApplicationConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean(name = "applicationTaskExecutor")
    Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(2); e.setMaxPoolSize(8); e.setQueueCapacity(500);
        e.setThreadNamePrefix("service-desk-");
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        e.initialize(); return e;
    }
}
```

## Testcontainers base

```java
package ru.miit.servicedesk;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
@Testcontainers
public abstract class PostgreSqlIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

## `.dockerignore`

```text
.git
.gradle
.idea
build
.env
Docs
```

## Dockerfile with wrapper

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Перед переносом проверить, что базовый образ содержит `useradd`; в Temurin Ubuntu обычно содержит, Alpine-вариант потребует `adduser`.
