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

@RestController
@RequestMapping("/apighost")
public class EngineController {

    private final ScenarioTestExecutor scenarioTestExecutor;
    private final FileLoaderEngine fileLoaderEngine;

    public EngineController(ScenarioTestExecutor scenarioTestExecutor,
        FileLoaderEngine fileLoaderEngine) {
        this.scenarioTestExecutor = scenarioTestExecutor;
        this.fileLoaderEngine = fileLoaderEngine;
    }

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

    @GetMapping("/scenario-list")
    public ResponseEntity<ScenarioListResponse> getScenarios() {
        ScenarioListResponse scenarioListResponse = fileLoaderEngine.getScenarioNames();
        return ResponseEntity.ok(scenarioListResponse);
    }

    @GetMapping("/result-list")
    public ResponseEntity<ScenarioResultListResponse> getScenarioResults() {
        ScenarioResultListResponse scenarioResultListResponse = fileLoaderEngine.getScenarioResults();
        return ResponseEntity.ok(scenarioResultListResponse);
    }
}
