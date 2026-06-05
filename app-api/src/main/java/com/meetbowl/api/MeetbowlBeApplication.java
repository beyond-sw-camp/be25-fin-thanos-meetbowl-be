package com.meetbowl.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.meetbowl")
public class MeetbowlBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetbowlBeApplication.class, args);
    }
}
