package com.cryptoBackend.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
        System.out.println("========================================");
        System.out.println("OpenEx Backend Started Successfully!");
        System.out.println("Health Check: http://localhost:8080/health");
        System.out.println("API Base URL: http://localhost:8080/api");
        System.out.println("========================================");
	}

}
