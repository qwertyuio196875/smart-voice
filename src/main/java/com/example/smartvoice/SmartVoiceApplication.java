package com.example.smartvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SmartVoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartVoiceApplication.class, args);
    }
}
