package com.exemple.quiz_app;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
@EnableScheduling
public class QuizAppApplication {

    @PostConstruct
    public void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Casablanca"));
    }

    public static void main(String[] args) {
        SpringApplication.run(QuizAppApplication.class, args);
    }

}
