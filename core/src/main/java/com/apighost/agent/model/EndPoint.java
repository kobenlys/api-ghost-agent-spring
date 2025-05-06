package com.apighost.agent.model;

import java.util.List;

public class EndPoint {

    private final String httpMethod;
    private final String path;
    private final List<String> produces;
    private final List<String> consumes;
    private final DtoSchema requestSchema;

    public EndPoint(String httpMethod, String path, List<String> produces, List<String> consumes,
        DtoSchema requestSchema) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
        this.requestSchema = requestSchema;
    }

    public String getHttpMethod() {
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

    public DtoSchema getRequestSchema() {
        return requestSchema;
    }

}
