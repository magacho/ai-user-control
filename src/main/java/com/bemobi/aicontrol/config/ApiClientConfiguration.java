package com.bemobi.aicontrol.config;

import com.bemobi.aicontrol.integration.claude.ClaudeApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for API clients.
 */
@Configuration
@EnableConfigurationProperties({
    ClaudeApiProperties.class
})
public class ApiClientConfiguration {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
