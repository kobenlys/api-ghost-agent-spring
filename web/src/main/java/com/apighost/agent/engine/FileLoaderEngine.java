package com.apighost.agent.engine;

import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.agent.file.FileLoader;
import com.apighost.agent.model.GenericFileDetailResponse;
import com.apighost.agent.model.ScenarioResultBrief;
import com.apighost.agent.model.ScenarioResultListResponse;
import com.apighost.agent.model.ScenarioListResponse;
import com.apighost.agent.util.ObjectMapperHolder;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.apighost.parser.scenario.reader.JsonScenarioResultReader;
import com.apighost.parser.scenario.reader.YamlScenarioReader;
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
    private final ApiGhostSetting apiGhostSetting;
    private static final Logger log = LoggerFactory.getLogger(FileLoaderEngine.class);

    public FileLoaderEngine(FileLoader fileLoader, ApiGhostSetting apiGhostSetting) {
        this.fileLoader = fileLoader;
        this.apiGhostSetting = apiGhostSetting;
        this.objectMapper = ObjectMapperHolder.getInstance();
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

            ScenarioResultBrief brief = new ScenarioResultBrief(file.getName(),
                scenarioResult.getIsScenarioSuccess(), scenarioResult.getExecutedAt());

            resultBriefs.add(brief);
        }

        return new ScenarioResultListResponse(resultBriefs);
    }

    /**
     * Retrieves a {@link Scenario} object by reading the YAML scenario file with the given name.
     * <p>
     * This method validates the input name, constructs the file path using {@link ApiGhostSetting}
     * </p>
     *
     * @param scenarioName the name of the scenario file to load (e.g., "login.yml")
     * @return the parsed {@link GenericFileDetailResponse} object
     * @throws IllegalArgumentException if the scenarioName is null, empty, or the file cannot be
     *                                  read
     */
    public GenericFileDetailResponse getScenarioInfo(String scenarioName) {
        YamlScenarioReader yamlScenarioReader = new YamlScenarioReader();
        if (isEmptyOrNull(scenarioName)) {
            throw new IllegalArgumentException("scenarioName must not be null or empty");
        }

        try {

            Scenario scenario = yamlScenarioReader.readScenario(
                apiGhostSetting.getScenarioPath() + "/" + scenarioName);
            return new GenericFileDetailResponse(scenarioName, scenario);
        } catch (IOException e) {
            log.error("ScenarioFile Not Found: {}", e.getMessage());
            throw new IllegalArgumentException("ScenarioFile Not Founded : " + scenarioName);
        }
    }

    /**
     * Retrieves a {@link ScenarioResult} object by reading the JSON test result file with the given
     * name.
     *
     * <p>
     * This method validates the input name, constructs the file path using {@link ApiGhostSetting}
     * </p>
     *
     * @param resultName the name of the result file to load (e.g., "login-result.json")
     * @return the parsed {@link GenericFileDetailResponse} object
     * @throws IllegalArgumentException if the resultName is null, empty, or the file cannot be
     *                                  read
     */
    public GenericFileDetailResponse getTestResultInfo(String resultName) {
        JsonScenarioResultReader jsonScenarioResultReader = new JsonScenarioResultReader();
        if (isEmptyOrNull(resultName)) {
            throw new IllegalArgumentException("resultName must not be null or empty");
        }

        if (!resultName.endsWith(apiGhostSetting.getFormatJson())) {
            resultName += apiGhostSetting.getFormatJson();
        }

        try {

            ScenarioResult scenarioResult = jsonScenarioResultReader.readScenarioResult(
                apiGhostSetting.getResultPath() + "/" + resultName);
            return new GenericFileDetailResponse(resultName, scenarioResult);
        } catch (IOException e) {
            log.error("ResultFile Not Found: {}", e.getMessage());
            throw new IllegalArgumentException("ResultFile Not Founded : " + resultName);
        }
    }

    private boolean isEmptyOrNull(String targetString) {
        return targetString == null || targetString.isEmpty();
    }
}
