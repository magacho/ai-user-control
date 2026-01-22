package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserDataTest {

    @Test
    void testUserDataCreation() {
        UserData userData = new UserData();

        assertNotNull(userData);
        assertNotNull(userData.getAdditionalMetrics());
        assertTrue(userData.getAdditionalMetrics().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        UserData userData = new UserData();

        String email = "test@example.com";
        String name = "Test User";
        String status = "active";
        LocalDateTime lastActivity = LocalDateTime.now();

        userData.setEmail(email);
        userData.setName(name);
        userData.setStatus(status);
        userData.setLastActivityAt(lastActivity);

        assertEquals(email, userData.getEmail());
        assertEquals(name, userData.getName());
        assertEquals(status, userData.getStatus());
        assertEquals(lastActivity, userData.getLastActivityAt());
    }

    @Test
    void testAdditionalMetrics() {
        UserData userData = new UserData();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("role", "admin");
        metrics.put("joined_at", LocalDateTime.now());

        userData.setAdditionalMetrics(metrics);

        assertEquals(2, userData.getAdditionalMetrics().size());
        assertEquals("admin", userData.getAdditionalMetrics().get("role"));
    }

    @Test
    void testRawJson() {
        UserData userData = new UserData();
        String rawJson = "{\"test\":\"data\"}";

        userData.setRawJson(rawJson);

        assertEquals(rawJson, userData.getRawJson());
    }

    @Test
    void testToString() {
        UserData userData = new UserData();
        userData.setEmail("test@example.com");
        userData.setName("Test User");
        userData.setStatus("active");

        String result = userData.toString();

        assertNotNull(result);
        assertTrue(result.contains("test@example.com"));
        assertTrue(result.contains("Test User"));
        assertTrue(result.contains("active"));
    }
}
