package com.gatekeepr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Einstiegspunkt für die GatekeepR-Middleware.
 * 
 * Diese Klasse bootstrapped die Anwendung und aktiviert den periodischen Scheduler.
 */
@SpringBootApplication
@EnableScheduling
public class GatekeepRApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatekeepRApplication.class, args);
    }
}
