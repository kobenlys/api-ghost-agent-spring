package com.apighost.agent.util;

import com.apighost.model.scenario.request.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Utility class for building HTTP requests based on a scenario {@link Request}.
 * <p>
 * This class follows the singleton pattern.
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class HttpRequestBuilder {

    private final ObjectMapper objectMapper;

    private HttpRequestBuilder() {
        this.objectMapper = ObjectMapperHolder.getInstance();
    }

    private static class SingletonHolder {

        private final static HttpRequestBuilder httpRequestBuilder = new HttpRequestBuilder();
    }

    /**
     * Returns the singleton instance of {@code HttpRequestBuilder}.
     *
     * @return the singleton instance
     */
    public static HttpRequestBuilder getInstance() {

        return SingletonHolder.httpRequestBuilder;
    }

    /**
     * Builds an {@link HttpEntity} based on the given scenario request.
     * <p>
     * For GET and DELETE methods, only headers are included.
     * For other methods, the body is parsed from JSON.
     *
     * @param request the scenario request object
     * @return an {@code HttpEntity} to be used with {@link org.springframework.web.client.RestTemplate}
     * @throws JsonProcessingException if the request body contains invalid JSON
     */
    public HttpEntity<?> build(Request request) throws JsonProcessingException {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (request.getHeader() != null) {
            request.getHeader().forEach(httpHeaders::add);
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod().name());

        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            return new HttpEntity<>(httpHeaders);
        } else {
            String jsonBody = request.getBody() == null ? "{}" : request.getBody().getJson();

            Map<String, Object> bodyMap =
                objectMapper.readValue(jsonBody,
                    new TypeReference<>() {
                    });
            return new HttpEntity<>(bodyMap, httpHeaders);
        }
    }
}
