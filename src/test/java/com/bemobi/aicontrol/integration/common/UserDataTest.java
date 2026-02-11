package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserDataTest {

    @Test
    void testUserDataCreation() {
        UserData userData = new UserData(null, null, null, null, null, null);

        assertNotNull(userData);
        assertNotNull(userData.additionalMetrics());
        assertTrue(userData.additionalMetrics().isEmpty());
    }

    @Test
    void testRecordAccessors() {
        String email = "test@example.com";
        String name = "Test User";
        String status = "active";
        LocalDateTime lastActivity = LocalDateTime.now();

        UserData userData = new UserData(email, name, status, lastActivity, null, null);

        assertEquals(email, userData.email());
        assertEquals(name, userData.name());
        assertEquals(status, userData.status());
        assertEquals(lastActivity, userData.lastActivityAt());
    }

    @Test
    void testAdditionalMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("role", "admin");
        metrics.put("joined_at", LocalDateTime.now());

        UserData userData = new UserData(null, null, null, null, metrics, null);

        assertEquals(2, userData.additionalMetrics().size());
        assertEquals("admin", userData.additionalMetrics().get("role"));
    }

    @Test
    void testRawJson() {
        String rawJson = "{\"test\":\"data\"}";

        UserData userData = new UserData(null, null, null, null, null, rawJson);

        assertEquals(rawJson, userData.rawJson());
    }

    @Test
    void testAdditionalMetricsIsImmutableWhenProvided() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("role", "admin");

        UserData userData = new UserData(null, null, null, null, metrics, null);

        assertThrows(UnsupportedOperationException.class, () ->
                userData.additionalMetrics().put("new_key", "value"));
    }

    @Test
    void testAdditionalMetricsIsImmutableWhenNull() {
        UserData userData = new UserData(null, null, null, null, null, null);

        assertThrows(UnsupportedOperationException.class, () ->
                userData.additionalMetrics().put("key", "value"));
    }

    @Test
    void testToString() {
        UserData userData = new UserData("test@example.com", "Test User", "active", null, null, null);

        String result = userData.toString();

        assertNotNull(result);
        assertTrue(result.contains("test@example.com"));
        assertTrue(result.contains("Test User"));
        assertTrue(result.contains("active"));
    }
}
