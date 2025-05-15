package com.apighost.agent.executor;

import com.apighost.agent.config.ApiGhostProperties;
import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.agent.model.ResponseResult;
import com.apighost.agent.util.ObjectMapperHolder;
import com.apighost.agent.util.TimeUtils;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.apighost.model.scenario.result.ResultStep;
import com.apighost.model.scenario.result.ResultStep.Builder;
import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;
import com.apighost.model.scenario.step.Route;
import com.apighost.model.scenario.step.Step;
import com.apighost.model.scenario.step.Then;
import com.apighost.parser.flattener.Flattener;
import com.apighost.parser.flattener.JsonFlattener;
import com.apighost.parser.template.TemplateConvertor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;

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
    private final HttpExecutor httpExecutor;
    private final Flattener flattener;

    private static final Logger log = LoggerFactory.getLogger(ScenarioTestExecutor.class);

    public ScenarioTestExecutor(ApiGhostSetting apiGhostSetting,
        ApiGhostProperties apiGhostProperties, HttpExecutor httpExecutor) {

        this.apiGhostSetting = apiGhostSetting;
        this.apiGhostProperties = apiGhostProperties;
        this.httpExecutor = httpExecutor;
        this.flattener = new JsonFlattener(ObjectMapperHolder.getInstance());
    }

    /**
     * Executes a scenario test, running through the steps defined in the scenario and executing
     * HTTP requests. The results are processed and stored step by step, and the final result is
     * returned as a {@link ScenarioResult}.
     *
     * <p>
     * The execution involves sending HTTP requests, matching the expected responses with
     * {@link Then} clauses, executing any actions defined in the {@link Then} clauses, and
     * proceeding to the next step based on conditions. If any failure occurs, the test execution
     * halts.
     * </p>
     *
     * @param scenario the scenario to execute, containing a sequence of steps to be tested
     * @param callback a callback function to process each step result
     * @return a {@link ScenarioResult} containing the outcome of the scenario execution, including
     * step results
     */
    public ScenarioResult testExecutor(Scenario scenario, Consumer<ResultStep> callback) {
        List<ResultStep> resultSteps = new ArrayList<>();
        Map<String, Object> store = new HashMap<>();

        long totalDurationMs = 0;
        long timeLimitMs = scenario.getTimeoutMs();
        boolean isTotalSuccess = true;

        log.info("Scenario Test Start - name: {}", scenario.getName());
        String nowStepName = scenario.getSteps().keySet().iterator().next();

        while (true) {
            Step presentStep = scenario.getSteps().get(nowStepName);
            ResponseResult responseResult =

                switch (presentStep.getType()) {
                    case HTTP -> httpExecutor.httpProtocolExecutor(presentStep.getRequest(), store,
                        timeLimitMs);
                    case WEBSOCKET ->
                        throw new UnsupportedOperationException("WS not implemented yet");
                };

            if (responseResult.getHttpStatus() == HttpStatus.REQUEST_TIMEOUT) {
                isTotalSuccess = false;
                ResultStep resultStep = buildResultStep(nowStepName, presentStep, responseResult,
                    null, null, "");
                callback.accept(resultStep);
                resultSteps.add(resultStep);
                break;
            }

            Map<String, String> flatResponseHeader = responseHeaderParser(
                responseResult.getHeader().map());
            Map<String, Object> flatResponseBody = null;
            if ("application/json".equals(flatResponseHeader.get("Content-Type"))) {
                flatResponseBody = flattener.flatten(responseResult.getBody());
            }

            Then matchedThen = matchsExpected(responseResult.getHttpStatus().value(),
                flatResponseBody, presentStep.getRoute());
            String nextStep =
                matchedThen == null ? null : executeThen(matchedThen, flatResponseBody, store);

            ResultStep resultStep = buildResultStep(nowStepName, presentStep, responseResult,
                flatResponseHeader, matchedThen, nextStep);

            totalDurationMs += responseResult.getDurationMs();
            timeLimitMs -= responseResult.getDurationMs();
            callback.accept(resultStep);
            resultSteps.add(resultStep);

            if (!scenario.getSteps().containsKey(nextStep)) {
                if (nextStep == null || nextStep.isEmpty()) {
                    isTotalSuccess = false;
                }
                break;
            }
            nowStepName = nextStep;
        }

        return new ScenarioResult.Builder().name(scenario.getName())
            .description(scenario.getDescription()).executedAt(TimeUtils.getNow())
            .totalDurationMs(totalDurationMs)
            .averageDurationMs(totalDurationMs / resultSteps.size())
            .filePath(apiGhostSetting.getResultPath()).baseUrl(apiGhostProperties.getBaseUrl())
            .isScenarioSuccess(isTotalSuccess).results(resultSteps).build();
    }

    /**
     * Matches the response status and body against the given route conditions and returns the
     * corresponding {@link Then} directive if a match is found.
     *
     * @param responseStatus   the HTTP status code returned from the response
     * @param flatResponseBody the flattened response body
     * @param routeList        the list of conditional routes defined for the step
     * @return the next {@link Then} directive to execute, or {@code null} if no match is found
     */
    private Then matchsExpected(int responseStatus, Map<String, Object> flatResponseBody,
        List<Route> routeList) {

        for (Route route : routeList) {
            if (route.getExpected() == null) {
                return route.getThen();
            }

            String expectedStatus = route.getExpected().getStatus();
            Map<String, Object> expectedBody = route.getExpected().getValue();

            if (isMatchExpectedStatus(responseStatus, expectedStatus)
                && isMatchExpectedValue(flatResponseBody, expectedBody)) {

                return route.getThen();
            }
        }
        return null;
    }

    /**
     * Checks whether the given status code matches the expected HTTP status or range.
     *
     * <p>If {@code expectedStatus} is null or empty, it returns {@code true}.</p>
     * <p>Supports patterns like {@code "200"} or {@code "200-299"}.</p>
     *
     * @param statusCode     the actual HTTP status code
     * @param expectedStatus the expected status code or range (e.g., "200", "200-299")
     * @return {@code true} if the status matches the expected pattern; otherwise {@code false}
     * @throws IllegalArgumentException if the pattern is invalid or contains non-numeric values
     */
    private boolean isMatchExpectedStatus(int statusCode, String expectedStatus) {
        if (expectedStatus == null || expectedStatus.isEmpty()) {
            return true;
        }

        String[] statusRange = expectedStatus.split("-");

        try {
            if (statusRange.length == 2) {
                if (Integer.parseInt(statusRange[0]) <= statusCode
                    && Integer.parseInt(statusRange[1]) >= statusCode) {
                    return true;
                }
            } else if (statusRange.length == 1) {
                if (Integer.parseInt(statusRange[0]) == statusCode) {
                    return true;
                }
            } else {
                throw new IllegalArgumentException(
                    "Invalid HTTP status pattern: " + expectedStatus);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid HTTP status pattern: " + expectedStatus, e);
        }

        return false;
    }

    /**
     * Validates whether the response body contains all expected key-value pairs.
     *
     * <p>If {@code flatResponseBody} or {@code expectedValue} is null or empty, it returns
     * {@code true}.</p>
     *
     * @param flatResponseBody the flattened response body map
     * @param expectedValue    the expected key-value pairs to verify
     * @return {@code true} if all expected values match; otherwise {@code false}
     */
    private boolean isMatchExpectedValue(Map<String, Object> flatResponseBody,
        Map<String, Object> expectedValue) {
        if (flatResponseBody == null || expectedValue == null || expectedValue.isEmpty()) {
            return true;
        }

        for (Entry<String, Object> entry : expectedValue.entrySet()) {
            if (!flatResponseBody.containsKey(entry.getKey())) {
                return false;
            }
            Object value = flatResponseBody.get(entry.getKey());
            if (!entry.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Stores selected values from the response body into a shared store based on the {@link Then}
     * definition.
     *
     * <p>Only keys defined in {@code matchedThen.getStore()} will be stored from
     * {@code responseBody}.</p>
     *
     * @param store        the store to update with response values
     * @param responseBody the full response body map
     * @param matchedThen  the {@link Then} object that specifies which keys to store
     */
    private void storedResponseValue(Map<String, Object> store, Map<String, Object> responseBody,
        Then matchedThen) {

        if (matchedThen == null || matchedThen.getStore() == null || matchedThen.getStore()
            .isEmpty()) {
            return;
        }

        Map<String, Object> expectedValue = matchedThen.getStore();
        for (Entry<String, Object> entry : responseBody.entrySet()) {
            if (expectedValue.containsKey(entry.getKey())) {
                store.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Executes a post-condition step defined in the {@link Then} object.
     *
     * <p>If {@code then.getStore()} is not null, performs template conversion using
     * {@code flatResponseBody} and {@code store}.</p>
     *
     * @param then             the {@link Then} definition containing the step and optional store
     * @param flatResponseBody the flattened response body used for variable resolution
     * @param store            the shared store used for template evaluation and value persistence
     * @return the next step defined in {@code then}, or {@code null} if not present
     */
    private String executeThen(Then then, Map<String, Object> flatResponseBody,
        Map<String, Object> store) {
        if (then == null || then.getStep() == null) {
            return null;
        }
        if (then.getStore() != null) {
            Map<String, Object> newStore = then.getStore();
            convertMapObjectTemplate(newStore, flatResponseBody);
            convertMapObjectTemplate(newStore, store);
            store.putAll(newStore);
        }

        return then.getStep();
    }

    /**
     * Performs template string conversion for all string values in the given map.
     *
     * <p>Each string value in {@code map} will be passed through {@link TemplateConvertor#convert}
     * using variables from {@code store}.</p>
     *
     * @param map   the map whose string values should be converted
     * @param store the store used to resolve placeholders in template strings
     */
    private void convertMapObjectTemplate(Map<String, Object> map, Map<String, Object> store) {
        if (map == null || map.isEmpty() || store == null || store.isEmpty()) {
            return;
        }

        for (Entry<String, Object> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof String originalValue)) {
                continue;
            }

            String convertedValue = TemplateConvertor.convert(originalValue, store);
            entry.setValue(convertedValue);
        }
    }

    private Map<String, String> responseHeaderParser(Map<String, List<String>> httpResponseHeader) {
        return httpResponseHeader.entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> String.join(", ", entry.getValue())
            ));
    }

    private ResultStep buildResultStep(String nowStepName, Step presentStep,
        ResponseResult responseResult, Map<String, String> flatResponseHeader, Then matchedThen,
        String nextStep) {

        return new Builder().stepName(nowStepName).type(presentStep.getType())
            .url(presentStep.getRequest().getUrl())
            .method(responseResult.getHttpMethod())
            .requestHeader(presentStep.getRequest().getHeader())
            .requestBody(presentStep.getRequest().getBody())
            .status(responseResult.getHttpStatus().value())
            .responseHeaders(flatResponseHeader)
            .responseBody(responseResult.getBody())
            .startTime(TimeUtils.convertFormat(responseResult.getStartTime()))
            .endTime(TimeUtils.convertFormat(responseResult.getEndTime()))
            .durationMs(responseResult.getDurationMs())
            .isRequestSuccess(matchedThen != null).route(presentStep.getRoute())
            .nextStep(nextStep).build();
    }
}
