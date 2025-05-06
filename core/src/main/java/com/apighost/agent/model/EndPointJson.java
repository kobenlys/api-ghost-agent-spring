package com.apighost.agent.model;

public class EndPointJson {
    private String HTTPMethod;
    private String path;
    private String jsonBody;

    public EndPointJson(String HTTPMethod, String path, String jsonBody) {
        this.HTTPMethod = HTTPMethod;
        this.path = path;
        this.jsonBody = jsonBody;
    }

    public String getHTTPMethod() {
        return HTTPMethod;
    }

    public String getPath() {
        return path;
    }

    public String getJsonBody() {
        return jsonBody;
    }
}
