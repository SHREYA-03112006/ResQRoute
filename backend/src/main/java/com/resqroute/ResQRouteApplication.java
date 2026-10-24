package com.resqroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ResQ-Route — AI-powered disaster logistics and safe-route planning system.
 *
 * <p>Entry point for the Spring Boot backend application.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class ResQRouteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResQRouteApplication.class, args);
    }
}
