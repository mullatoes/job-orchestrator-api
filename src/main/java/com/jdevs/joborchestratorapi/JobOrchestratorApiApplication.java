package com.jdevs.joborchestratorapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JobOrchestratorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobOrchestratorApiApplication.class, args);
    }

}
