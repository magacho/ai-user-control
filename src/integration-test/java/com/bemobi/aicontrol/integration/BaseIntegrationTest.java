package com.bemobi.aicontrol.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that make real API calls.
 *
 * These tests require real credentials to be configured via environment variables.
 * They are tagged with "integration" and can be run separately from unit tests.
 *
 * To run integration tests:
 * mvn verify -P integration-tests
 *
 * Or with Maven:
 * mvn test -Dgroups=integration
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@Tag("integration")
public abstract class BaseIntegrationTest {

    /**
     * Checks if an environment variable is configured.
     */
    protected boolean isConfigured(String envVar) {
        String value = System.getenv(envVar);
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Gets an environment variable value.
     */
    protected String getEnv(String envVar) {
        return System.getenv(envVar);
    }
}
