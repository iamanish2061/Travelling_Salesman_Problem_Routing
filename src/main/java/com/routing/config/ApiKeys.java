package com.routing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "opencage")
public class ApiKeys {
    private String apiKey;

    public ApiKeys() {
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public ApiKeys(String openCageKey) {
        this.apiKey = openCageKey;
    }
}
