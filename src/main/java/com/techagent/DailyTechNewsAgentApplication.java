package com.techagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DailyTechNewsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyTechNewsAgentApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  Daily Top Tech News AI Agent Started!");
        System.out.println("  Open: http://localhost:8080");
        System.out.println("========================================\n");
    }
}
