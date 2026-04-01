package com.dealersac.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DealersAcApplication {
    public static void main(String[] args) {
        SpringApplication.run(DealersAcApplication.class, args);
    }
}
