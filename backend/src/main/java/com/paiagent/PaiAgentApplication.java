package com.paiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PaiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaiAgentApplication.class, args);
    }
}
