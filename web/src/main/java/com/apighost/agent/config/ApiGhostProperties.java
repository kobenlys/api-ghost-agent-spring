package com.apighost.agent.config;

import org.springframework.stereotype.Component;

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
