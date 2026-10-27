package com.xin.aiagentbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiAgentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentBackendApplication.class, args);
    }

}
