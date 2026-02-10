package com.bemobi.aicontrol.config;

import com.bemobi.aicontrol.integration.claude.ClaudeApiProperties;
import com.bemobi.aicontrol.integration.cursor.CursorApiProperties;
import com.bemobi.aicontrol.integration.github.GitHubApiProperties;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for API clients.
 */
@Configuration
@EnableConfigurationProperties({
    ClaudeApiProperties.class,
    GitHubApiProperties.class,
    CursorApiProperties.class,
    GoogleWorkspaceProperties.class
})
public class ApiClientConfiguration {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
