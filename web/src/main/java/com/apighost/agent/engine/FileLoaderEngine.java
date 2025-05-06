package com.apighost.agent.engine;

import com.apighost.agent.executor.ScenarioTestExecutor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLoaderEngine {

    private final FileLoader fileLoader;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ScenarioTestExecutor.class);

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

        for (File file : resultFiles) {
            ScenarioResult scenarioResult;
            try {
                scenarioResult = objectMapper.readValue(file, ScenarioResult.class);

            } catch (IOException e) {
                log.error("Failed to parse result file: {}", file.getAbsolutePath());
                continue;
            }

            ScenarioResultBrief brief = new ScenarioResultBrief(scenarioResult.getName(),
                scenarioResult.isScenarioSuccess(), scenarioResult.getExecutedAt());

            resultBriefs.add(brief);
        }

        return new ScenarioResultListResponse(resultBriefs);
    }
}
