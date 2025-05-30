package com.apighost.agent.config;

import org.springframework.stereotype.Component;

/**
 * Configuration properties for the API Ghost application.
 * <p>
 * This component holds configurable settings such as the base package to scan and the OpenAI API
 * key for integration.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
@Component
public class ApiGhostProperties {

    private String basePackage;
    private String baseUrl;
    private String openAiKey;

    public ApiGhostProperties(String basePackage, String baseUrl, String openAiKey) {
        this.basePackage = basePackage;
        this.baseUrl = baseUrl;
        this.openAiKey = openAiKey;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getOpenAiKey() {
        return openAiKey;
    }

    public void setOpenAiKey(String openAiKey) {
        this.openAiKey = openAiKey;
    }
}
