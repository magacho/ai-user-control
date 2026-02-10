package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionTestResultTest {

    @Test
    void testSuccessWithDefaultMessage() {
        ConnectionTestResult result = ConnectionTestResult.success("test-tool");

        assertNotNull(result);
        assertEquals("test-tool", result.toolName());
        assertTrue(result.success());
        assertEquals("Connection successful", result.message());
    }

    @Test
    void testSuccessWithCustomMessage() {
        String customMessage = "Connection established successfully";
        ConnectionTestResult result = ConnectionTestResult.success("test-tool", customMessage);

        assertNotNull(result);
        assertEquals("test-tool", result.toolName());
        assertTrue(result.success());
        assertEquals(customMessage, result.message());
    }

    @Test
    void testFailure() {
        String errorMessage = "Connection timeout";
        ConnectionTestResult result = ConnectionTestResult.failure("test-tool", errorMessage);

        assertNotNull(result);
        assertEquals("test-tool", result.toolName());
        assertFalse(result.success());
        assertEquals(errorMessage, result.message());
    }

    @Test
    void testToString() {
        ConnectionTestResult result = ConnectionTestResult.success("test-tool");

        String resultString = result.toString();

        assertNotNull(resultString);
        assertTrue(resultString.contains("test-tool"));
        assertTrue(resultString.contains("success=true"));
    }
}
