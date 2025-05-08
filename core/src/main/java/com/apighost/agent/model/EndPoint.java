package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;

import java.util.List;

/**
 * Represents an API endpoint with protocol, method, path, media types, and parameter information.
 *
 * <p>This class is used to describe API metadata for scenario execution or documentation purposes.</p>
 *
 * @author oneweeek
 * @version BETA-0.0.1
 */
public class EndPoint {

    private final ProtocolType protocolType;
    private final String baseUrl;
    private final String methodName;
    private final HTTPMethod httpMethod;
    private final String path;
    private final List<String> produces;
    private final List<String> consumes;
    private final List<FieldMeta> requestSchema;
    private final List<FieldMeta> responseSchema;
    private final List<Parameter> headers;
    private final List<Parameter> cookies;
    private final List<Parameter> requestParams;
    private final List<Parameter> pathVariables;

    /**
     * Constructs an {@code EndPoint} using the provided {@link Builder}.
     *
     * @param builder the builder containing endpoint values
     */
    public EndPoint(Builder builder) {
        this.protocolType = builder.protocolType;
        this.baseUrl = builder.baseUrl;
        this.methodName = builder.methodName;
        this.httpMethod = builder.httpMethod;
        this.path = builder.path;
        this.produces = builder.produces;
        this.consumes = builder.consumes;
        this.requestSchema = builder.requestSchema;
        this.responseSchema = builder.responseSchema;
        this.headers = builder.headers;
        this.cookies = builder.cookies;
        this.requestParams = builder.requestParams;
        this.pathVariables = builder.pathVariables;
    }

    /**
     * Builder class for constructing {@link EndPoint} instances.
     */
    public static class Builder {
        private ProtocolType protocolType;
        private String baseUrl;
        private String methodName;
        private HTTPMethod httpMethod;
        private String path;
        private List<String> produces;
        private List<String> consumes;
        private List<FieldMeta> requestSchema;
        private List<FieldMeta> responseSchema;
        private List<Parameter> headers;
        private List<Parameter> cookies;
        private List<Parameter> requestParams;
        private List<Parameter> pathVariables;

        public Builder protocolType(ProtocolType protocolType) {
            this.protocolType = protocolType;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder httpMethod(HTTPMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder produces(List<String> produces) {
            this.produces = produces;
            return this;
        }

        public Builder consumes(List<String> consumes) {
            this.consumes = consumes;
            return this;
        }

        public Builder requestSchema(List<FieldMeta> requestSchema) {
            this.requestSchema = requestSchema;
            return this;
        }

        public Builder responseSchema(List<FieldMeta> responseSchema) {
            this.responseSchema = responseSchema;
            return this;
        }

        public Builder headers(List<Parameter> headers) {
            this.headers = headers;
            return this;
        }

        public Builder cookies(List<Parameter> cookies) {
            this.cookies = cookies;
            return this;
        }

        public Builder requestParams(List<Parameter> requestParams) {
            this.requestParams = requestParams;
            return this;
        }

        public Builder pathVariables(List<Parameter> pathVariables) {
            this.pathVariables = pathVariables;
            return this;
        }

        /**
         * Builds and returns an {@link EndPoint} instance.
         *
         * @return the constructed EndPoint
         */
        public EndPoint build() {
            return new EndPoint(this);
        }
    }

    /** @return the protocol type (e.g., HTTP, HTTPS) */
    public ProtocolType getProtocolType() {
        return protocolType;
    }

    /** @return the base URL of the endpoint */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** @return the method name associated with the endpoint */
    public String getMethodName() {
        return methodName;
    }

    /** @return the HTTP method (GET, POST, etc.) */
    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    /** @return the request path of the endpoint */
    public String getPath() {
        return path;
    }

    /** @return the media types that the endpoint can produce */
    public List<String> getProduces() {
        return produces;
    }

    /** @return the media types that the endpoint can consume */
    public List<String> getConsumes() {
        return consumes;
    }

    /** @return the schema of the request body */
    public List<FieldMeta> getRequestSchema() {
        return requestSchema;
    }

    /** @return the schema of the response body */
    public List<FieldMeta> getResponseSchema() {
        return responseSchema;
    }

    /** @return the list of header parameters */
    public List<Parameter> getHeaders() {
        return headers;
    }

    /** @return the list of cookie parameters */
    public List<Parameter> getCookies() {
        return cookies;
    }

    /** @return the list of request parameters */
    public List<Parameter> getRequestParams() {
        return requestParams;
    }

    /** @return the list of path variables */
    public List<Parameter> getPathVariables() {
        return pathVariables;
    }
}