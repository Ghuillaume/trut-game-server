package com.trutgame.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrutGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrutGameApplication.class, args);
    }
}
