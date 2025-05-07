package com.apighost.agent.controller;

import com.apighost.agent.engine.FileLoaderEngine;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.model.ScenarioListResponse;
import com.apighost.agent.model.ScenarioResultListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * Constructs a new {@code EngineController} with the required executor and engine.
     *
     * @param scenarioTestExecutor the component responsible for executing scenario tests
     * @param fileLoaderEngine     the engine responsible for loading scenario files and results
     */
    public EngineController(ScenarioTestExecutor scenarioTestExecutor,
        FileLoaderEngine fileLoaderEngine) {
        this.scenarioTestExecutor = scenarioTestExecutor;
        this.fileLoaderEngine = fileLoaderEngine;
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
}
