package com.bemobi.aicontrol.integration.common;

/**
 * Resultado de um teste de conexão com uma API externa.
 */
public class ConnectionTestResult {

    private final String toolName;
    private final boolean success;
    private final String message;

    private ConnectionTestResult(String toolName, boolean success, String message) {
        this.toolName = toolName;
        this.success = success;
        this.message = message;
    }

    public static ConnectionTestResult success(String toolName) {
        return new ConnectionTestResult(toolName, true, "Connection successful");
    }

    public static ConnectionTestResult success(String toolName, String message) {
        return new ConnectionTestResult(toolName, true, message);
    }

    public static ConnectionTestResult failure(String toolName, String message) {
        return new ConnectionTestResult(toolName, false, message);
    }

    public String getToolName() {
        return toolName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ConnectionTestResult{" +
                "toolName='" + toolName + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
