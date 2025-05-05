package com.apighost.agent.executor;

import com.apighost.agent.loader.ScenarioFileLoader;
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
import java.util.Date;
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


@Component
public class ScenarioTestExecutor {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ScenarioFileLoader scenarioFileLoader;

    private static final Logger log = LoggerFactory.getLogger(ScenarioTestExecutor.class);
    private String baseFilePath;

    public ScenarioTestExecutor(RestTemplate restTemplate, ObjectMapper objectMapper,
        ScenarioFileLoader scenarioFileLoader, String baseFilePath) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.scenarioFileLoader = scenarioFileLoader;
        this.baseFilePath = baseFilePath;
    }

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

                ResultStep resultStep = new Builder()
                    .stepName(beforeStepName)
                    .type(ProtocolType.HTTP)
                    .url(presentStep.getRequest().getUrl())
                    .method(responseResult.getHttpMethod())
                    .requestHeader(presentStep.getRequest().getHeader())
                    .requestBody(presentStep.getRequest().getBody())
                    .status(responseResult.getHttpStatus().value())
                    .responseHeaders(responseResult.getHeader())
                    .responseBody(objectMapper.writeValueAsString(responseResult.getBody()))
                    .startTime(responseResult.getStartTime().toString())
                    .endTime(responseResult.getEndTime().toString())
                    .durationMs(responseResult.getDurationMs())
                    .requestSuccess(matchedThen != null)
                    .route(null)
                    .build();

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

        ScenarioResult scenarioResult = new ScenarioResult.Builder()
            .name(scenarioInfo.getName())
            .description(scenarioInfo.getDescription())
            .executedAt(String.valueOf(new Date()))
            .totalDurationMs(totalDurationMs)
            .averageDurationMs(stepCount == 0 ? 0 : totalDurationMs / stepCount)
            .filePath(baseFilePath)
            .baseUrl("/localhost:8080")
            .isScenarioSuccess(isTotalSuccess)
            .results(resultSteps)
            .build();

        emitter.send(SseEmitter.event().name("complete").data(scenarioResult));
    }

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

                Map<String, Object> bodyMap = objectMapper.readValue(
                    scenarioRequest.getBody().getJson(), new TypeReference<>() {
                    });

                httpEntity = new HttpEntity<>(bodyMap, headers);
            }

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(scenarioRequest.getUrl(),
                httpMethod, httpEntity, String.class);
            long endTime = System.currentTimeMillis();

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            Map<String, Object> resBody;

            if (jsonNode.isObject()) {
                resBody = objectMapper.readValue(response.getBody(),
                    new TypeReference<>() {
                    });
            } else {
                resBody = Collections.emptyMap();
            }

            HttpStatusCode httpStatus = HttpStatus.resolve(response.getStatusCode().value());
            HttpHeaders responseHeaders = response.getHeaders();

            Map<String, String> resHeader = new HashMap<>();
            for (Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    resHeader.put(entry.getKey(), entry.getValue().get(0));
                }
            }

            return new ResponseResult.Builder()
                .header(resHeader)
                .body(resBody)
                .httpStatus(httpStatus)
                .httpMethod(scenarioRequest.getMethod())
                .startTime(new Date(startTime))
                .endTime(new Date(endTime))
                .durationMs((int) (endTime - startTime))
                .build();

        } catch (JsonProcessingException e) {
            log.error("Error - json parsing : {}", e.getMessage());
            return null;
        }
    }
}
