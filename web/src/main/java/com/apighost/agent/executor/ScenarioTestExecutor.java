package com.apighost.agent.executor;

import com.apighost.agent.config.ApiGhostProperties;
import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.apighost.model.scenario.result.ResultStep;
import com.apighost.model.scenario.step.Step;
import com.apighost.scenario.executor.StepExecutor;
import com.apighost.scenario.executor.WebSocketStepExecutor;
import com.apighost.validator.ScenarioValidator;

import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Executes scenario-based HTTP tests by reading and running steps defined in YAML files. Sends
 * step-wise and final test results via Server-Sent Events (SSE).
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ScenarioTestExecutor {

    private final ApiGhostSetting apiGhostSetting;
    private final ApiGhostProperties apiGhostProperties;
    private final StepExecutor httpStepExecutor;
    private final StepExecutor webSocketStepExecutor;

    private static final Logger log = LoggerFactory.getLogger(ScenarioTestExecutor.class);

    public ScenarioTestExecutor(ApiGhostSetting apiGhostSetting,
        ApiGhostProperties apiGhostProperties, StepExecutor http, StepExecutor webSocket) {

        this.apiGhostSetting = apiGhostSetting;
        this.apiGhostProperties = apiGhostProperties;
        this.httpStepExecutor = http;
        this.webSocketStepExecutor = webSocket;
    }

    /**
     * Executes the provided scenario and returns the result.
     *
     * @param scenario the scenario to execute
     * @param callback optional callback invoked after each step and at the end of the scenario
     * @return the {@link ScenarioResult} containing execution outcomes
     */
    public ScenarioResult testExecutor(Scenario scenario, Consumer<ResultStep> callback) {
        ScenarioValidator.validateScenarioForExecution(scenario);
        ScenarioValidator.validateNoRouteCycle(scenario,
            scenario.getSteps().keySet().iterator().next());

        List<ResultStep> resultStepList = new ArrayList<>();
        Map<String, Object> store =
            scenario.getStore() != null ? scenario.getStore() : new HashMap<>();
        LinkedHashMap<String, Step> steps = scenario.getSteps();

        boolean isAllScenarioSuccess = true;
        long totalDurationMs = 0;
        long remainTimeoutMs = scenario.getTimeoutMs();
        String currentStepKey = steps.keySet().iterator().next();
        log.info("Execute Scenario Test : " + scenario.getName());
        while (currentStepKey != null) {

            Step currentStep = steps.get(currentStepKey);
            ResultStep resultStep;

            try {
                resultStep = switch (currentStep.getType()) {
                    case HTTP -> httpStepExecutor.execute(currentStepKey, currentStep, store,
                        remainTimeoutMs);
                    case WEBSOCKET ->
                        webSocketStepExecutor.execute(currentStepKey, currentStep, store,
                            remainTimeoutMs);
                };
            } catch (Exception e) {
                resultStep = new ResultStep.Builder()
                    .stepName(currentStepKey)
                    .type(currentStep.getType())
                    .url(currentStep.getRequest().getUrl())
                    .method(currentStep.getRequest().getMethod())
                    .url(currentStep.getRequest().getUrl())
                    .requestHeader(currentStep.getRequest().getHeader())
                    .requestBody(currentStep.getRequest().getBody())
                    .route(currentStep.getRoute())
                    .build();
            }

            if (!resultStep.getIsRequestSuccess()) {
                isAllScenarioSuccess = false;
            }

            resultStepList.add(resultStep);
            totalDurationMs += resultStep.getDurationMs();
            remainTimeoutMs -= resultStep.getDurationMs();
            callback.accept(resultStep);
            currentStepKey = resultStep.getNextStep();

        }

        WebSocketStepExecutor.clearAll();

        return new ScenarioResult.Builder()
            .name(scenario.getName())
            .description(scenario.getDescription())
            .executedAt(Instant.now().toString())
            .baseUrl(apiGhostProperties.getBaseUrl())
            .filePath(apiGhostSetting.getResultPath())
            .totalDurationMs(totalDurationMs)
            .averageDurationMs(
                resultStepList.isEmpty() ? 0 : totalDurationMs / resultStepList.size())
            .isScenarioSuccess(isAllScenarioSuccess)
            .results(resultStepList)
            .build();
    }
}
