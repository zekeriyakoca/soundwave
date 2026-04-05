package com.soundwave.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
public abstract class MariaDbIntegrationTestBase {

    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11")
            .withDatabaseName("soundwave")
            .withUsername("soundwave")
            .withPassword("soundwave");

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

}
