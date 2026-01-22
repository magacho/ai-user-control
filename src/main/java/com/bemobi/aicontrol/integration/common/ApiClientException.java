package com.bemobi.aicontrol.integration.common;

/**
 * Exception thrown when there's an error communicating with external AI tool APIs.
 */
public class ApiClientException extends Exception {

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
