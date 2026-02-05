package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.common.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for collecting user data from all AI tool integrations.
 *
 * This service coordinates the data collection from Claude Code, GitHub Copilot,
 * and Cursor APIs, consolidating results into a unified structure.
 */
@Service
public class UserCollectionService {

    private static final Logger log = LoggerFactory.getLogger(UserCollectionService.class);

    private final List<ToolApiClient> apiClients;

    /**
     * Constructs the UserCollectionService with all available API clients.
     *
     * @param apiClients List of ToolApiClient implementations injected by Spring
     */
    public UserCollectionService(List<ToolApiClient> apiClients) {
        this.apiClients = apiClients;
        log.info("UserCollectionService initialized with {} API clients", apiClients.size());
    }

    /**
     * Collects user data from all enabled AI tool integrations.
     *
     * This method iterates through all registered API clients, collects data from enabled ones,
     * and consolidates the results into a map keyed by tool name.
     *
     * @return Map with tool names as keys and lists of UserData as values
     */
    public Map<String, List<UserData>> collectAllUsers() {
        log.info("Starting user collection from all integrations");
        Map<String, List<UserData>> results = new HashMap<>();
        int totalUsers = 0;

        for (ToolApiClient client : apiClients) {
            String toolName = client.getToolName();

            if (!client.isEnabled()) {
                log.info("Skipping {} - integration is disabled", toolName);
                results.put(toolName, new ArrayList<>());
                continue;
            }

            try {
                log.info("Collecting users from {}...", client.getDisplayName());
                List<UserData> users = client.fetchUsers();
                results.put(toolName, users);
                totalUsers += users.size();
                log.info("Successfully collected {} users from {}", users.size(), client.getDisplayName());
            } catch (Exception e) {
                log.error("Error collecting users from {}: {}", client.getDisplayName(), e.getMessage(), e);
                results.put(toolName, new ArrayList<>());
            }
        }

        log.info("User collection completed. Total users collected: {} from {} integrations",
                totalUsers, apiClients.size());
        return results;
    }

    /**
     * Collects user data from a specific tool by name.
     *
     * @param toolName The name of the tool (e.g., "claude", "github-copilot", "cursor")
     * @return List of UserData for the specified tool, or empty list if not found/disabled
     */
    public List<UserData> collectFromTool(String toolName) {
        log.info("Collecting users from specific tool: {}", toolName);

        for (ToolApiClient client : apiClients) {
            if (client.getToolName().equalsIgnoreCase(toolName)) {
                if (!client.isEnabled()) {
                    log.warn("{} integration is disabled", client.getDisplayName());
                    return new ArrayList<>();
                }

                try {
                    List<UserData> users = client.fetchUsers();
                    log.info("Collected {} users from {}", users.size(), client.getDisplayName());
                    return users;
                } catch (Exception e) {
                    log.error("Error collecting users from {}: {}", client.getDisplayName(), e.getMessage(), e);
                    return new ArrayList<>();
                }
            }
        }

        log.warn("Tool '{}' not found or not registered", toolName);
        return new ArrayList<>();
    }

    /**
     * Collects user data from Claude Code integration.
     *
     * @return List of UserData from Claude Code, or empty list if disabled/error
     */
    public List<UserData> collectFromClaude() {
        return collectFromTool("claude");
    }

    /**
     * Collects user data from GitHub Copilot integration.
     *
     * @return List of UserData from GitHub Copilot, or empty list if disabled/error
     */
    public List<UserData> collectFromGitHub() {
        return collectFromTool("github-copilot");
    }

    /**
     * Collects user data from Cursor integration.
     *
     * @return List of UserData from Cursor, or empty list if disabled/error
     */
    public List<UserData> collectFromCursor() {
        return collectFromTool("cursor");
    }

    /**
     * Returns the list of all registered API clients.
     *
     * @return List of ToolApiClient instances
     */
    public List<ToolApiClient> getApiClients() {
        return apiClients;
    }
}
