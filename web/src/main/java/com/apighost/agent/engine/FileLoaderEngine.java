package com.apighost.agent.engine;

import com.apighost.agent.loader.FileLoader;
import com.apighost.agent.model.ScenarioResultBrief;
import com.apighost.agent.model.ScenarioResultListResponse;
import com.apighost.agent.model.ScenarioListResponse;
import com.apighost.model.scenario.ScenarioResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileLoaderEngine {

    private final FileLoader fileLoader;
    private final ObjectMapper objectMapper;

    public FileLoaderEngine(FileLoader fileLoader, ObjectMapper objectMapper) {
        this.fileLoader = fileLoader;
        this.objectMapper = objectMapper;
    }

    public ScenarioListResponse getScenarioNames() {
        return new ScenarioListResponse(fileLoader.getScenarioNames());
    }

    public ScenarioResultListResponse getScenarioResults() {
        List<ScenarioResultBrief> resultBriefs = new ArrayList<>();
        List<File> resultFiles = fileLoader.getScenarioResults();

        try {
            for (File file : resultFiles) {
                ScenarioResult scenarioResult = objectMapper.readValue(file, ScenarioResult.class);

                ScenarioResultBrief brief = new ScenarioResultBrief(scenarioResult.getName(),
                    scenarioResult.isScenarioSuccess(), scenarioResult.getExecutedAt());
                resultBriefs.add(brief);
            }

            return new ScenarioResultListResponse(resultBriefs);

        } catch (IOException e) {
            throw new IllegalStateException("An error occurred while parsing the JSON");
        }
    }
}
