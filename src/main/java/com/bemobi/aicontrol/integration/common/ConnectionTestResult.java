package com.bemobi.aicontrol.integration.common;

/**
 * Resultado de um teste de conexão com uma API externa.
 */
public record ConnectionTestResult(String toolName, boolean success, String message) {

    public static ConnectionTestResult success(String toolName) {
        return new ConnectionTestResult(toolName, true, "Connection successful");
    }

    public static ConnectionTestResult success(String toolName, String message) {
        return new ConnectionTestResult(toolName, true, message);
    }

    public static ConnectionTestResult failure(String toolName, String message) {
        return new ConnectionTestResult(toolName, false, message);
    }
}
