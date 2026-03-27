package com.campus.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campus.platform.repository")
@EnableScheduling
public class CampusPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusPlatformApplication.class, args);
    }
}
