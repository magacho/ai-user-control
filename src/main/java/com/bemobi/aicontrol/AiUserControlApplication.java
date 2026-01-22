package com.bemobi.aicontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for AI User Control system.
 *
 * This application provides CLI tools to collect and consolidate
 * user information from AI development tools (Claude Code, GitHub Copilot, Cursor).
 */
@SpringBootApplication
public class AiUserControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiUserControlApplication.class, args);
    }
}
