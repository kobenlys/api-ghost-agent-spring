package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;

public class EndPointJson {

    private HTTPMethod httpMethod;
    private String path;
    private String jsonBody;

    public EndPointJson(HTTPMethod httpMethod, String path, String jsonBody) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.jsonBody = jsonBody;
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getJsonBody() {
        return jsonBody;
    }
}
