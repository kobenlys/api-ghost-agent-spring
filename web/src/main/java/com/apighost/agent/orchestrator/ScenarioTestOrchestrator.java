package com.apighost.agent.orchestrator;

import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.file.FileExporter;
import com.apighost.agent.file.ScenarioFileLoader;
import com.apighost.agent.notifier.ScenarioResultNotifier;
import com.apighost.agent.util.YamlMapperHolder;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ScenarioTestOrchestrator {

    private final ScenarioFileLoader scenarioFileLoader;
    private final ScenarioTestExecutor scenarioTestExecutor;
    private final FileExporter fileExporter;
    private final ApiGhostSetting setting;
    private final ObjectMapper yamlObjectMapper;

    public ScenarioTestOrchestrator(ScenarioFileLoader scenarioFileLoader,
        ScenarioTestExecutor scenarioTestExecutor, ApiGhostSetting apiGhostSetting) {

        this.scenarioFileLoader = scenarioFileLoader;
        this.scenarioTestExecutor = scenarioTestExecutor;
        this.fileExporter = new FileExporter();
        this.setting = apiGhostSetting;
        this.yamlObjectMapper = YamlMapperHolder.getInstance();
    }

    public void executeScenario(String scenarioName, ScenarioResultNotifier resultNotifier) {

        Scenario scenario = loadScenario(scenarioName);
        ScenarioResult scenarioResult = scenarioTestExecutor.testExecutor(scenario,
            resultNotifier::notifyStep);
        fileExporter.exportFile(scenarioResult, setting.getFormatJson(), setting.getResultPath());

        resultNotifier.notifyCompletion(scenarioResult);
    }

    private Scenario loadScenario(String scenarioName) {

        try {
            File scenarioFile = scenarioFileLoader.findScenarioFile(scenarioName);
            return yamlObjectMapper.readValue(scenarioFile, Scenario.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to Load Scenario");
        }
    }

}
