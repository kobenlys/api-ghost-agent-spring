package com.apighost.agent.controller;

import com.apighost.agent.config.ApiGhostProperties;
import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.agent.engine.FileLoaderEngine;
import com.apighost.agent.file.FileExporter;
import com.apighost.agent.file.FileRemover;
import com.apighost.agent.model.ScenarioExportResponse;
import com.apighost.agent.model.ScenarioListResponse;
import com.apighost.agent.model.ScenarioResultListResponse;
import com.apighost.agent.notifier.ResultSseNotifier;
import com.apighost.agent.notifier.ScenarioResultNotifier;
import com.apighost.agent.orchestrator.ScenarioTestOrchestrator;
import com.apighost.model.GenerateBody;
import com.apighost.model.scenario.Scenario;
import com.apighost.orchestrator.DataGenerationOrchestrator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller that exposes endpoints for managing and executing API Ghost scenarios.
 * <p>
 * This controller provides endpoints to:
 * <ul>
 *     <li>Execute a scenario test with real-time results via Server-Sent Events (SSE)</li>
 *     <li>Retrieve the list of available test scenarios</li>
 *     <li>Retrieve the list of completed scenario test results</li>
 * </ul>
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
@RestController
@RequestMapping("/apighost")
public class EngineController {

    private final ScenarioTestOrchestrator scenarioTestOrchestrator;
    private final DataGenerationOrchestrator dataGenerationOrchestrator;
    private final FileLoaderEngine fileLoaderEngine;
    private final ApiGhostProperties apiGhostProperties;
    private final ApiGhostSetting apiGhostSetting;

    public EngineController(ScenarioTestOrchestrator scenarioTestOrchestrator,
        DataGenerationOrchestrator dataGenerationOrchestrator, FileLoaderEngine fileLoaderEngine,
        ApiGhostSetting apiGhostSetting, ApiGhostProperties apiGhostProperties) {

        this.scenarioTestOrchestrator = scenarioTestOrchestrator;
        this.dataGenerationOrchestrator = dataGenerationOrchestrator;
        this.fileLoaderEngine = fileLoaderEngine;
        this.apiGhostSetting = apiGhostSetting;
        this.apiGhostProperties = apiGhostProperties;
    }

    /**
     * Executes a scenario test and streams real-time results to the client using Server-Sent Events
     * (SSE).
     * <p>
     * This endpoint initializes an {@link SseEmitter} to asynchronously send updates about the
     * scenario execution. Events are sent as the test progresses, and include step results and a
     * completion signal.
     * </p>
     *
     * @param scenarioName the name of the scenario to be executed (without file extension)
     * @return an {@link SseEmitter} for streaming scenario execution updates to the client
     */
    @GetMapping("/scenario-test")
    public SseEmitter scenarioExecutor(@RequestParam("scenarioName") String scenarioName) {

        SseEmitter sseEmitter = new SseEmitter();
        ScenarioResultNotifier notifier = new ResultSseNotifier(sseEmitter);
        scenarioTestOrchestrator.executeScenario(scenarioName, notifier);
        return sseEmitter;
    }

    /**
     * Retrieves the list of available scenario test files.
     *
     * @return a {@link ResponseEntity} containing the list of scenario names
     */
    @GetMapping("/scenario-list")
    public ResponseEntity<ScenarioListResponse> getScenarios() {
        ScenarioListResponse scenarioListResponse = fileLoaderEngine.getScenarioNames();
        return ResponseEntity.ok(scenarioListResponse);
    }

    /**
     * Retrieves the list of stored scenario test results.
     *
     * @return a {@link ResponseEntity} containing the list of scenario result summaries
     */
    @GetMapping("/result-list")
    public ResponseEntity<ScenarioResultListResponse> getScenarioResults() {
        ScenarioResultListResponse scenarioResultListResponse =
            fileLoaderEngine.getScenarioResults();
        return ResponseEntity.ok(scenarioResultListResponse);
    }

    /**
     * Retrieve detailed information about a specific scenario.
     *
     * @param scenarioName the name of the scenario file to retrieve information for
     * @return a {@link ResponseEntity} containing the scenario information, or an appropriate error
     * response
     */
    @GetMapping("/scenario-info")
    public ResponseEntity<?> getScenarioInfo(@RequestParam("scenarioName") String scenarioName) {
        return ResponseEntity.ok(fileLoaderEngine.getScenarioInfo(scenarioName));
    }

    /**
     * Retrieve test result details based on a specific result file name.
     *
     * @param testResultName the name of the test result file to retrieve
     * @return a {@link ResponseEntity} containing the test result information, or an appropriate
     * error response
     */
    @GetMapping("/result-info")
    public ResponseEntity<?> getResultInfo(@RequestParam("testResultName") String testResultName) {
        return ResponseEntity.ok(fileLoaderEngine.getTestResultInfo(testResultName));
    }

    /**
     * Exports the provided scenario to a file using the configured format and path.
     * <p>
     * The scenario is saved in either YAML or JSON format depending on the current settings.
     * </p>
     *
     * @param scenario the {@link Scenario} object to be exported
     * @return a {@link ResponseEntity} containing the export result, including file name and status
     */
    @PostMapping("/scenario-export")
    public ResponseEntity<ScenarioExportResponse> exportScenarioFile(
        @RequestBody Scenario scenario) {
        FileExporter fileExporter = new FileExporter();
        return ResponseEntity.ok(
            fileExporter.safeExportFile(scenario, apiGhostSetting.getFormatYaml(),
                apiGhostSetting.getScenarioPath()));
    }


    @PostMapping("/generate-data")
    public ResponseEntity<GenerateBody> generateData(@RequestBody GenerateBody generateBody) {
        return ResponseEntity.ok(
            dataGenerationOrchestrator.executeGenerate(generateBody.getJsonBody(),
                apiGhostProperties.getOpenAiKey()));
    }

    /**
     * Deletes a file with the specified file name under the apighost directory.
     *
     * @param fileName the name of the file to be deleted, including extension (e.g.
     *                 "example.yaml")
     * @return {@link ResponseEntity} containing {@code true} if the file was deleted successfully,
     * or {@code false} if deletion failed or the file does not exist
     */
    @DeleteMapping("/file-remove/{fileName:.+}")
    public ResponseEntity<Boolean> removeFile(@PathVariable("fileName") String fileName) {
        FileRemover fileRemover = new FileRemover(apiGhostSetting);
        return ResponseEntity.ok(fileRemover.remove(fileName));
    }
}
