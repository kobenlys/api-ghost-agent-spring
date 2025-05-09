package com.apighost.agent.file;

import com.apighost.agent.config.ApiGhostSetting;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Loads scenario files and result files from predefined directories.
 * <p>
 * This class uses {@link ApiGhostSetting} to locate scenario and result file paths, and filters
 * files based on configured file extensions.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class FileLoader {

    private final ApiGhostSetting apiGhostSetting;

    /**
     * Constructs a new {@code FileLoader} with the given settings.
     *
     * @param apiGhostSetting the configuration that provides file paths and extensions
     */
    public FileLoader(ApiGhostSetting apiGhostSetting) {
        this.apiGhostSetting = apiGhostSetting;
    }

    /**
     * Retrieves the list of available scenario file names from the configured scenario directory.
     * <p>
     * Only files with configured YAML or YML extensions are included.
     * </p>
     *
     * @return a list of scenario file names
     * @throws IllegalStateException if the scenario directory is not found
     */
    public List<String> getScenarioNames() {
        List<String> scenarioNames = new ArrayList<>();

        File scenarioDirectory = new File(apiGhostSetting.getScenarioPath());
        if (!scenarioDirectory.isDirectory()) {
            throw new IllegalStateException("Scenario directory not found");
        }

        File[] files = scenarioDirectory.listFiles();
        if (files == null) {
            return scenarioNames;
        }

        for (File file : files) {
            if (file.isFile() && (file.getName()
                .endsWith(apiGhostSetting.getFormatYaml()) || file.getName()
                .endsWith(apiGhostSetting.getFormatYml()))) {
                scenarioNames.add(file.getName());
            }
        }

        return scenarioNames;
    }

    /**
     * Retrieves the list of result files from the configured result directory.
     * <p>
     * Only files with configured JSON extension are included.
     * </p>
     *
     * @return a list of result files
     * @throws IllegalStateException if the result directory is not found
     */
    public List<File> getScenarioResults() {
        List<File> scenarioResultFiles = new ArrayList<>();

        File resultDirectory = new File(apiGhostSetting.getResultPath());
        if (!resultDirectory.isDirectory()) {
            throw new IllegalStateException("Result directory not found");
        }

        File[] files = resultDirectory.listFiles();
        if (files == null) {
            return scenarioResultFiles;
        }

        for (File file : files) {
            if (file.isFile() && (file.getName().endsWith(apiGhostSetting.getFormatJson()))) {
                scenarioResultFiles.add(file);
            }
        }
        return scenarioResultFiles;
    }
}
