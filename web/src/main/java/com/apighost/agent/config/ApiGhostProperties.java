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
    private String openAiKey;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getOpenAiKey() {
        return openAiKey;
    }

    public void setOpenAiKey(String openAiKey) {
        this.openAiKey = openAiKey;
    }
}
