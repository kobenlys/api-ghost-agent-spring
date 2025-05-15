package com.apighost.agent.executor;

import com.apighost.agent.model.ResponseResult;
import com.apighost.agent.util.ExecutionTimer;
import com.apighost.agent.util.HttpRequestBuilder;
import com.apighost.agent.util.ObjectMapperHolder;
import com.apighost.model.scenario.request.Request;
import com.apighost.parser.template.TemplateConvertor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
 * This class utilizes a {@link RestTemplate} to perform HTTP exchanges, parses the responses, and
 * returns structured results including status code, headers, body, and execution duration. It
 * supports parsing of common JSON types as well as plain text responses.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class HttpExecutor {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final HttpRequestBuilder httpRequestBuilder;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(HttpExecutor.class);

    public HttpExecutor() {
        this.httpRequestBuilder = HttpRequestBuilder.getInstance();
        this.objectMapper = ObjectMapperHolder.getInstance();
    }

    /**
     * Executes an HTTP request defined in the scenario {@link Request} and captures the response.
     *
     * @param request the HTTP request definition in the scenario
     * @return a {@link ResponseResult} containing the status, headers, body, and duration
     */
    public ResponseResult httpProtocolExecutor(Request request, Map<String, Object> store,
        long timeoutMs) {

        HttpRequest.Builder builder;
        try {
            builder = HttpRequest.newBuilder().uri(URI.create(request.getUrl()))
                .timeout(java.time.Duration.ofMillis(timeoutMs));

        } catch (IllegalArgumentException e) {
            return null;
        }

        if (request.getHeader() != null) {
            convertMapStringTemplate(request.getHeader(), store);
            request.getHeader().forEach(builder::header);
        }

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
        if (request.getBody() != null) {
            if (request.getBody().getJson() != null) {
                body = HttpRequest.BodyPublishers.ofString(
                    TemplateConvertor.convert(request.getBody().getJson(), store));
            }
        }

        HttpRequest httpRequest;
        switch (request.getMethod()) {
            case GET -> httpRequest = builder.GET().build();
            case POST -> httpRequest = builder.POST(body).build();
            case PUT -> httpRequest = builder.PUT(body).build();
            case DELETE, PATCH, HEAD, OPTIONS, TRACE, CONNECT ->
                httpRequest = builder.method(request.getMethod().name(), body).build();
            default -> throw new UnsupportedOperationException(
                "Unknown method: " + request.getMethod());
        }

        ExecutionTimer.DurationResult<HttpResponse<String>> response = ExecutionTimer.execute(
            () ->
            {
                try {
                    return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                }catch (HttpTimeoutException e){
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );

        if (response.result == null) {
            return new ResponseResult.Builder().header(null)
                .body("")
                .httpStatus(HttpStatus.REQUEST_TIMEOUT).httpMethod(request.getMethod())
                .startTime(0).endTime(0)
                .durationMs(0).build();
        }

        String responseBodyStr = response.result.body();
        HttpStatusCode httpStatus = HttpStatus.resolve(response.result.statusCode());

        return new ResponseResult.Builder().header(response.result.headers())
            .body(responseBodyStr)
            .httpStatus(httpStatus).httpMethod(request.getMethod())
            .startTime(response.startTime).endTime(response.endTime)
            .durationMs(response.durationTime).build();
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

    private void convertMapStringTemplate(Map<String, String> map, Map<String, Object> store) {
        if (map == null || map.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String originalValue = entry.getValue();
            String convertedValue = TemplateConvertor.convert(originalValue, store);
            entry.setValue(convertedValue);
        }
    }
}
