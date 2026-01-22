package com.bemobi.aicontrol.integration.common;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO base para dados de usuário coletados de ferramentas de IA.
 *
 * Este DTO normaliza informações de diferentes plataformas em um formato comum.
 */
public class UserData {

    private String email;
    private String name;
    private String status;
    private LocalDateTime lastActivityAt;
    private Map<String, Object> additionalMetrics;
    private String rawJson; // Dados brutos para debug

    public UserData() {
        this.additionalMetrics = new HashMap<>();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Map<String, Object> getAdditionalMetrics() {
        return additionalMetrics;
    }

    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) {
        this.additionalMetrics = additionalMetrics;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", lastActivityAt=" + lastActivityAt +
                ", additionalMetrics=" + additionalMetrics +
                '}';
    }
}
