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

/**
 * Engine that loads scenario definitions and test results using the {@link FileLoader}.
 * <p>
 * It converts raw file data into structured response models used by the API layer.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class FileLoaderEngine {

    private final FileLoader fileLoader;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ScenarioTestExecutor.class);

    /**
     * Constructs a new {@code FileLoaderEngine} with the specified dependencies.
     *
     * @param fileLoader   the file loader for accessing scenario and result files
     * @param objectMapper the object mapper used to deserialize result files
     */
    public FileLoaderEngine(FileLoader fileLoader, ObjectMapper objectMapper) {
        this.fileLoader = fileLoader;
        this.objectMapper = objectMapper;
    }

    public ScenarioListResponse getScenarioNames() {
        return new ScenarioListResponse(fileLoader.getScenarioNames());
    }

    /**
     * Retrieves the list of scenario result summaries.
     * <p>
     * Parses each result file into a {@link ScenarioResult}, extracts key metadata, and returns a
     * list of brief summaries.
     * </p>
     *
     * @return a {@link ScenarioResultListResponse} containing brief result information
     */
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
