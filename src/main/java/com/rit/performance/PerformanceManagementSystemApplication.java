package com.rit.performance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PerformanceManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PerformanceManagementSystemApplication.class, args);
	}

}
