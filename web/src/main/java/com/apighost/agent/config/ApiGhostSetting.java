package com.apighost.agent.config;

import org.springframework.stereotype.Component;

@Component
public class ApiGhostSetting {

    private final String scenarioPath;
    private final String resultPath;
    private final String formatYaml;
    private final String formatYml;
    private final String formatJson;

    public ApiGhostSetting(String scenarioPath, String resultPath, String formatYaml,
        String formatYml,
        String formatJson) {
        this.scenarioPath = scenarioPath;
        this.resultPath = resultPath;
        this.formatYaml = formatYaml;
        this.formatYml = formatYml;
        this.formatJson = formatJson;
    }

    public String getScenarioPath() {
        return scenarioPath;
    }

    public String getResultPath() {
        return resultPath;
    }

    public String getFormatYaml() {
        return formatYaml;
    }

    public String getFormatYml() {
        return formatYml;
    }

    public String getFormatJson() {
        return formatJson;
    }
}
