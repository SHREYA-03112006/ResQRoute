package com.resqroute;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies that the Spring application context loads without errors.
 *
 * <p>Uses in-memory/test properties to avoid requiring a live database or Redis
 * during CI or when running tests without infrastructure.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.redis.host=localhost",
        "spring.cache.type=none",
        "resqroute.tinyfish.mock-mode=true"
})
class ResQRouteApplicationTests {

    @Test
    void contextLoads() {
        // If the application context starts, this test passes.
    }
}
