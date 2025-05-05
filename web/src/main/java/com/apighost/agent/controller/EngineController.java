package com.apighost.agent.controller;

import com.apighost.agent.executor.ScenarioTestExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/apighost")
public class EngineController {

    private final ScenarioTestExecutor scenarioTestExecutor;

    public EngineController(ScenarioTestExecutor scenarioTestExecutor) {
        this.scenarioTestExecutor = scenarioTestExecutor;
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

}
