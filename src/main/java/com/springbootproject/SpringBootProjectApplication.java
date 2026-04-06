package com.springbootproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories("com.springbootproject.Repository")
@EntityScan("com.springbootproject.Entity")
@EnableScheduling
public class SpringBootProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootProjectApplication.class, args);
    }

}
