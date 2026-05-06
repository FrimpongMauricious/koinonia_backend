// Entry point. @SpringBootApplication enables component scan, auto-configuration, and @Configuration scanning.
package com.koinonia.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KoinoniaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KoinoniaApplication.class, args);
    }
}
