package com.apighost.agent.controller;

import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.agent.engine.FileLoaderEngine;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.file.FileExporter;
import com.apighost.agent.model.ScenarioExportResponse;
import com.apighost.agent.model.ScenarioListResponse;
import com.apighost.agent.model.ScenarioResultListResponse;
import com.apighost.model.scenario.Scenario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    private final ScenarioTestExecutor scenarioTestExecutor;
    private final FileLoaderEngine fileLoaderEngine;
    private final FileExporter fileExporter;
    private final ApiGhostSetting apiGhostSetting;
    /**
     * Constructs a new {@code EngineController} with the required executor and engine.
     *
     * @param scenarioTestExecutor the component responsible for executing scenario tests
     * @param fileLoaderEngine     the engine responsible for loading scenario files and results
     */
    public EngineController(ScenarioTestExecutor scenarioTestExecutor,
        FileLoaderEngine fileLoaderEngine, FileExporter fileExporter,
        ApiGhostSetting apiGhostSetting) {
        this.scenarioTestExecutor = scenarioTestExecutor;
        this.fileLoaderEngine = fileLoaderEngine;
        this.fileExporter = fileExporter;
        this.apiGhostSetting = apiGhostSetting;
    }

    /**
     * Executes a specified scenario test and streams its result steps and summary through SSE.
     * <p>
     * The scenario is processed in a separate thread to allow non-blocking result streaming.
     * Clients can consume events named <code>stepResult</code> and <code>complete</code>.
     * </p>
     *
     * @param scenarioName the name of the scenario (without file extension)
     * @return an {@link SseEmitter} that streams the test execution results
     */
    @GetMapping("/scenario-test")
    public SseEmitter scenarioExecutor(@RequestParam("scenarioName") String scenarioName) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                scenarioTestExecutor.testExecutor(scenarioName, emitter);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
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
    public ResponseEntity<ScenarioExportResponse> exportScenarioFile(@RequestBody Scenario scenario){
        return ResponseEntity.ok(fileExporter.safeExportFile(scenario, apiGhostSetting.getFormatYaml(),
            apiGhostSetting.getScenarioPath()));
    }
}
