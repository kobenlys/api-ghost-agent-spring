package com.apighost.agent.executor;

import com.apighost.agent.config.ApiGhostSetting;
import com.apighost.agent.file.FileExporter;
import com.apighost.agent.file.ScenarioFileLoader;
import com.apighost.agent.model.ResponseResult;
import com.apighost.agent.util.TimeUtils;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.apighost.model.scenario.request.Request;
import com.apighost.model.scenario.result.ResultStep;
import com.apighost.model.scenario.result.ResultStep.Builder;
import com.apighost.model.scenario.step.ProtocolType;
import com.apighost.model.scenario.step.Route;
import com.apighost.model.scenario.step.Step;
import com.apighost.model.scenario.step.Then;
import com.apighost.parser.flattener.Flattener;
import com.apighost.parser.flattener.JsonFlattener;
import com.apighost.parser.template.TemplateConvertor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


/**
 * Executes scenario-based HTTP tests by reading and running steps defined in YAML files. Sends
 * step-wise and final test results via Server-Sent Events (SSE).
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
@Component
public class ScenarioTestExecutor {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ScenarioFileLoader scenarioFileLoader;
    private final FileExporter fileExporter;
    private final ApiGhostSetting apiGhostSetting;
    private Flattener flattener;

    private static final Logger log = LoggerFactory.getLogger(ScenarioTestExecutor.class);

    /**
     * Constructs a new {@code ScenarioTestExecutor} with required dependencies.
     *
     * @param restTemplate       the HTTP client used to send requests during scenario execution
     * @param objectMapper       the JSON object mapper used for serializing and deserializing data
     * @param scenarioFileLoader the component responsible for loading scenario definitions
     * @param apiGhostSetting    the configuration settings for API Ghost execution environment
     */
    public ScenarioTestExecutor(RestTemplate restTemplate, ObjectMapper objectMapper,
        ScenarioFileLoader scenarioFileLoader, FileExporter fileExporter,
        ApiGhostSetting apiGhostSetting) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.scenarioFileLoader = scenarioFileLoader;
        this.fileExporter = fileExporter;
        this.apiGhostSetting = apiGhostSetting;
    }

    /**
     * Executes a scenario test specified by its name and emits step-by-step results via SSE.
     *
     * @param scenarioName the name of the scenario file (without ".yaml")
     * @param emitter      the {@link SseEmitter} used to send results in real time
     * @throws IOException if the scenario file cannot be read or parsed
     */
    public void testExecutor(String scenarioName, SseEmitter emitter) throws IOException {
        List<ResultStep> resultSteps = new ArrayList<>();
        Map<String, Object> store = new HashMap<>();
        flattener = new JsonFlattener(objectMapper);

        Scenario scenarioInfo = null;
        long totalDurationMs = 0;
        int stepCount = 0;
        boolean isTotalSuccess = true;
        log.info("Scenario Test Start - name: {}", scenarioName);
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            File scenarioFile = scenarioFileLoader.findScenarioFile(scenarioName);
            scenarioInfo = mapper.readValue(scenarioFile, Scenario.class);

            if (scenarioInfo.getSteps().isEmpty()) {
                return;
            }

            String beforeStepName = scenarioInfo.getSteps().keySet().iterator().next();
            Step presentStep = scenarioInfo.getSteps().values().iterator().next();

            while (true) {
                ResponseResult responseResult = httpProtocolExecutor(presentStep.getRequest());
                if (responseResult == null) {
                    break;
                }

                Map<String, Object> flatResponseBody = flattener.flatten(
                    objectMapper.writeValueAsString(responseResult.getBody()));

                Then matchedThen = matchsExpected(responseResult.getHttpStatus().value(),
                    flatResponseBody, presentStep.getRoute());

                String nextStep =
                    matchedThen == null ? null : executeThen(matchedThen, flatResponseBody, store);

                ResultStep resultStep =
                    new Builder().stepName(beforeStepName).type(ProtocolType.HTTP)
                        .url(presentStep.getRequest().getUrl())
                        .method(responseResult.getHttpMethod())
                        .requestHeader(presentStep.getRequest().getHeader())
                        .requestBody(presentStep.getRequest().getBody())
                        .status(responseResult.getHttpStatus().value())
                        .responseHeaders(responseResult.getHeader())
                        .responseBody(objectMapper.writeValueAsString(responseResult.getBody()))
                        .startTime(TimeUtils.convertFormat(responseResult.getStartTime()))
                        .endTime(TimeUtils.convertFormat(responseResult.getEndTime()))
                        .durationMs(responseResult.getDurationMs())
                        .isRequestSuccess(matchedThen != null).route(presentStep.getRoute())
                        .nextStep(nextStep).build();

                stepCount++;
                totalDurationMs += responseResult.getDurationMs();
                emitter.send(SseEmitter.event().name("stepResult").data(resultStep));
                resultSteps.add(resultStep);

                if (!scenarioInfo.getSteps().containsKey(nextStep)) {
                    if (nextStep == null) {
                        isTotalSuccess = false;
                    }
                    break;
                }

                storedResponseValue(store, responseResult.getBody(), matchedThen);
                beforeStepName = nextStep;
                presentStep = scenarioInfo.getSteps().get(nextStep);
            }
        } catch (IOException e) {
            log.error("Error - Scenario-test : {}", e.getMessage());
            isTotalSuccess = false;
        }

        ScenarioResult scenarioResult = new ScenarioResult.Builder().name(scenarioInfo.getName())
            .description(scenarioInfo.getDescription()).executedAt(TimeUtils.getNow())
            .totalDurationMs(totalDurationMs)
            .averageDurationMs(stepCount == 0 ? 0 : totalDurationMs / stepCount)
            .filePath(apiGhostSetting.getResultPath()).baseUrl("/localhost:8080")
            .isScenarioSuccess(isTotalSuccess).results(resultSteps).build();

        fileExporter.exportFile(scenarioResult, apiGhostSetting.getFormatJson(),
            apiGhostSetting.getResultPath());
        emitter.send(SseEmitter.event().name("complete").data(scenarioResult));
    }

    /**
     * Executes an HTTP request defined in the scenario {@link Request} and captures the response.
     *
     * @param scenarioRequest the HTTP request definition in the scenario
     * @return a {@link ResponseResult} containing the status, headers, body, and duration
     */
    private ResponseResult httpProtocolExecutor(Request scenarioRequest) {

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> httpEntity;
            HttpMethod httpMethod = HttpMethod.valueOf(scenarioRequest.getMethod().name());
            if (scenarioRequest.getHeader() != null) {
                scenarioRequest.getHeader().forEach(headers::add);
            }

            if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
                httpEntity = new HttpEntity<>(headers);
            } else {

                Map<String, Object> bodyMap =
                    objectMapper.readValue(scenarioRequest.getBody().getJson(),
                        new TypeReference<>() {
                        });

                httpEntity = new HttpEntity<>(bodyMap, headers);
            }

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response =
                restTemplate.exchange(scenarioRequest.getUrl(), httpMethod, httpEntity,
                    String.class);
            long endTime = System.currentTimeMillis();

            String responseBodyStr = response.getBody();
            Map<String, Object> resBody = getParseBody(responseBodyStr);
            HttpStatusCode httpStatus = HttpStatus.resolve(response.getStatusCode().value());
            HttpHeaders responseHeaders = response.getHeaders();

            Map<String, String> resHeader = new HashMap<>();
            for (Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    resHeader.put(entry.getKey(), entry.getValue().get(0));
                }
            }

            return new ResponseResult.Builder().header(resHeader).body(resBody)
                .httpStatus(httpStatus).httpMethod(scenarioRequest.getMethod())
                .startTime(startTime).endTime(endTime)
                .durationMs((int) (endTime - startTime)).build();

        } catch (JsonProcessingException e) {
            log.error("Error - json parsing : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses a raw HTTP response body string into a {@code Map<String, Object>} representation.
     * Supports JSON object, string, boolean, number, and handles plain text gracefully.
     *
     * @param responseBodyStr the raw HTTP response body as a string
     * @return a parsed body as a map
     * @throws JsonProcessingException if the input is a malformed JSON string
     */
    private Map<String, Object> getParseBody(String responseBodyStr)
        throws JsonProcessingException {
        Map<String, Object> resBody;

        if (!isJson(responseBodyStr)) {
            return Map.of("value", responseBodyStr);
        }

        if (responseBodyStr == null || responseBodyStr.trim().isEmpty()) {
            resBody = Collections.emptyMap();
        } else {

            try {
                JsonNode jsonNode = objectMapper.readTree(responseBodyStr);

                return switch (jsonNode.getNodeType()) {
                    case OBJECT -> objectMapper.readValue(responseBodyStr, new TypeReference<>() {
                    });
                    case BOOLEAN -> Map.of("value", jsonNode.asBoolean());
                    case NUMBER -> Map.of("value", jsonNode.numberValue());
                    case STRING -> Map.of("value", jsonNode.asText());
                    default -> Collections.emptyMap();
                };
            } catch (JsonProcessingException e) {
                return Map.of("value", responseBodyStr);
            }
        }
        return resBody;
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

        for (Map.Entry<String, Object> entry : expectedValue.entrySet()) {
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

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof String originalValue)) {
                continue;
            }

            String convertedValue = TemplateConvertor.convert(originalValue, store);
            entry.setValue(convertedValue);
        }
    }

    /**
     * Checks whether a given string can be interpreted as a valid JSON content.
     *
     * @param targetString the string to check
     * @return {@code true} if the string is likely JSON, {@code false} otherwise
     */
    private boolean isJson(String targetString) {
        targetString = targetString.trim();

        return (targetString.startsWith("{") && targetString.endsWith(
            "}")) || (targetString.startsWith("[") && targetString.endsWith(
            "]")) || targetString.equals("false") || targetString.equals("null");
    }
}
