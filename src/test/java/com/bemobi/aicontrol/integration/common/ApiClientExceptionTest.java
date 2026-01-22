package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "API call failed";
        ApiClientException exception = new ApiClientException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionWithMessageAndCause() {
        String message = "API call failed";
        Throwable cause = new RuntimeException("Network error");

        ApiClientException exception = new ApiClientException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        assertEquals("Network error", exception.getCause().getMessage());
    }
}
