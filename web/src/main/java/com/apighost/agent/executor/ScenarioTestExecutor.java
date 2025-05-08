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
        Scenario scenarioInfo = null;
        long totalDurationMs = 0;
        int stepCount = 0;
        boolean isTotalSuccess = true;
        log.info("Scenario Test Start - name: {}", scenarioName + ".yaml");
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

                Then matchedThen = matchsExpected(responseResult, presentStep.getRoute());

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
                        .isRequestSuccess(matchedThen != null).route(null).build();

                stepCount++;
                totalDurationMs += responseResult.getDurationMs();
                emitter.send(SseEmitter.event().name("stepResult").data(resultStep));
                resultSteps.add(resultStep);

                if (matchedThen == null) {
                    isTotalSuccess = false;
                    break;
                }

                if (!scenarioInfo.getSteps().containsKey(matchedThen.getStep())) {
                    break;
                }
                beforeStepName = matchedThen.getStep();
                presentStep = scenarioInfo.getSteps().get(matchedThen.getStep());
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
     * Matches the actual response against the list of defined {@link Route} conditions, returning
     * the corresponding {@link Then} directive if expectations are met.
     *
     * @param responseResult the actual response received from executing the step
     * @param routeList      the list of conditional routing definitions in the scenario
     * @return the matched {@link Then} step to execute next, or {@code null} if no match found
     */
    private Then matchsExpected(ResponseResult responseResult, List<Route> routeList) {
        Map<String, Object> responseBody = responseResult.getBody();

        for (Route route : routeList) {

            if (route.getExpected() == null || route.getExpected().getValue() == null) {
                return route.getThen();
            }

            int status = Integer.parseInt(route.getExpected().getStatus());
            Map<String, Object> expectedBody = route.getExpected().getValue();

            if (responseResult.getHttpStatus().value() == status) {
                boolean allMatch = true;

                for (Entry<String, Object> entry : expectedBody.entrySet()) {
                    String key = entry.getKey();
                    Object expectedValue = entry.getValue();

                    if (!responseBody.containsKey(key)) {
                        allMatch = false;
                        break;
                    }

                    Object actualValue = responseBody.get(key);
                    if (!expectedValue.equals(actualValue)) {
                        allMatch = false;
                        break;
                    }
                }

                if (allMatch) {
                    return route.getThen();
                }
            }
        }
        return null;
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
