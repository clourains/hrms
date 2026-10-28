package com.erpdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ErpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpDemoApplication.class, args);
    }
}
