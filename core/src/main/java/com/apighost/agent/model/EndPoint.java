package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;
import java.util.List;

public class EndPoint {

    private final ProtocolType protocolType;
    private final String methodName;
    private final HTTPMethod httpMethod;
    private final String path;
    private final List<String> produces;
    private final List<String> consumes;
    private final DtoSchema requestSchema;
    private final DtoSchema responseSchema;

    public EndPoint(ProtocolType protocolType, String methodName, HTTPMethod httpMethod, String path, List<String> produces,
        List<String> consumes, DtoSchema requestSchema, DtoSchema responseSchema) {
        this.protocolType = protocolType;
        this.methodName = methodName;
        this.httpMethod = httpMethod;
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
        this.requestSchema = requestSchema;
        this.responseSchema = responseSchema;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
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

    public DtoSchema getRequestSchema() {
        return requestSchema;
    }

    public DtoSchema getResponseSchema(){
        return responseSchema;
    }
}
