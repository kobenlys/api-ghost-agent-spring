package com.apighost.agent.executor;

import com.apighost.agent.model.ResponseResult;
import com.apighost.agent.util.ExecutionTimer;
import com.apighost.agent.util.HttpRequestBuilder;
import com.apighost.agent.util.ObjectMapperHolder;
import com.apighost.model.scenario.request.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Executes HTTP requests based on scenario definitions and collects response metadata.
 * <p>
 * This class utilizes a {@link RestTemplate} to perform HTTP exchanges, parses the responses,
 * and returns structured results including status code, headers, body, and execution duration.
 * It supports parsing of common JSON types as well as plain text responses.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class HttpExecutor {

    private final RestTemplate restTemplate;
    private final HttpRequestBuilder httpRequestBuilder;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(HttpExecutor.class);

    public HttpExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.httpRequestBuilder = HttpRequestBuilder.getInstance();
        this.objectMapper = ObjectMapperHolder.getInstance();
    }

    /**
     * Executes an HTTP request defined in the scenario {@link Request} and captures the response.
     *
     * @param request the HTTP request definition in the scenario
     * @return a {@link ResponseResult} containing the status, headers, body, and duration
     */
    public ResponseResult httpProtocolExecutor(Request request) {

        try {
            HttpEntity<?> httpEntity = httpRequestBuilder.build(request);
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().name());

            ExecutionTimer.DurationResult<ResponseEntity<String>> response = ExecutionTimer.execute(
                () ->
                    restTemplate.exchange(request.getUrl(), httpMethod, httpEntity,
                        String.class)
            );

            String responseBodyStr = response.result.getBody();
            Map<String, Object> resBody = getParseBody(responseBodyStr);
            HttpStatusCode httpStatus = HttpStatus.resolve(response.result.getStatusCode().value());
            HttpHeaders responseHeaders = response.result.getHeaders();

            Map<String, String> resHeader = new HashMap<>();
            for (Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    resHeader.put(entry.getKey(), entry.getValue().get(0));
                }
            }

            return new ResponseResult.Builder().header(resHeader).body(resBody)
                .httpStatus(httpStatus).httpMethod(request.getMethod())
                .startTime(response.startTime).endTime(response.endTime)
                .durationMs((int) response.durationTime).build();

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
