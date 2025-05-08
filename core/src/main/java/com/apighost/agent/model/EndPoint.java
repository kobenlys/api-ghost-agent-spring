package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;
import java.util.List;

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

        public EndPoint build() {
            return new EndPoint(this);
        }
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getMethodName() {
        return methodName;
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public List<String> getProduces() {
        return produces;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public List<FieldMeta> getRequestSchema() {
        return requestSchema;
    }

    public List<FieldMeta> getResponseSchema() {
        return responseSchema;
    }

    public List<Parameter> getHeaders() {
        return headers;
    }

    public List<Parameter> getCookies() {
        return cookies;
    }

    public List<Parameter> getRequestParams() {
        return requestParams;
    }

    public List<Parameter> getPathVariables() {
        return pathVariables;
    }
}
