package com.apighost.agent.loader;

import com.apighost.agent.config.ApiGhostSetting;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FileLoader {

    private final ApiGhostSetting apiGhostSetting;

    public FileLoader(ApiGhostSetting apiGhostSetting) {
        this.apiGhostSetting = apiGhostSetting;
    }

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
            if (file.isFile() && (file.getName().endsWith(apiGhostSetting.getFormatYaml())
                || file.getName().endsWith(apiGhostSetting.getFormatYml()))) {
                scenarioNames.add(file.getName());
            }
        }

        return scenarioNames;
    }

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
