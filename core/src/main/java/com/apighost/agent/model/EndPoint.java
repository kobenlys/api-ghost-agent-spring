package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;
import java.util.List;

public class EndPoint {

    private final HTTPMethod httpMethod;
    private final String path;
    private final List<String> produces;
    private final List<String> consumes;
    private final DtoSchema requestSchema;

    public EndPoint(HTTPMethod httpMethod, String path, List<String> produces, List<String> consumes,
        DtoSchema requestSchema) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
        this.requestSchema = requestSchema;
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

    public DtoSchema getRequestSchema() {
        return requestSchema;
    }

}
