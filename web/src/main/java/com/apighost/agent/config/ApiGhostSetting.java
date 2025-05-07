package com.apighost.agent.config;

import org.springframework.stereotype.Component;

/**
 * Configuration component that holds API Ghost scenario and result paths, along with supported file
 * formats.
 * <p>
 * This component provides path settings and format identifiers that are used throughout the
 * scenario execution lifecycle.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
@Component
public class ApiGhostSetting {

    private final String scenarioPath;
    private final String resultPath;
    private final String formatYaml;
    private final String formatYml;
    private final String formatJson;

    /**
     * Constructs a new {@code ApiGhostSetting} with provided scenario path, result path, and format
     * identifiers.
     *
     * @param scenarioPath the file system path where scenario files are stored
     * @param resultPath   the file system path where test result files are stored
     * @param formatYaml   the extension string for YAML format (e.g., ".yaml")
     * @param formatYml    the extension string for YML format (e.g., ".yml")
     * @param formatJson   the extension string for JSON format (e.g., ".json")
     */
    public ApiGhostSetting(String scenarioPath, String resultPath, String formatYaml,
        String formatYml, String formatJson) {
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
