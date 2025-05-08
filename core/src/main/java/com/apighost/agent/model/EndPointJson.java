package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;

/**
 * Represents an HTTP endpoint with its method, path, and request body in JSON format.
 *
 * <p>This class is used to describe an API endpoint for scenario or test execution.</p>
 *
 * @author oneweeek
 * @version BETA-0.0.1
 */
public class EndPointJson {

    private HTTPMethod httpMethod;
    private String path;
    private String jsonBody;

    /**
     * Constructs a new {@code EndPointJson} with the specified HTTP method, path, and JSON body.
     *
     * @param httpMethod the HTTP method
     * @param path the request path
     * @param jsonBody the request body in JSON format
     */
    public EndPointJson(HTTPMethod httpMethod, String path, String jsonBody) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.jsonBody = jsonBody;
    }

    /**
     * Returns the HTTP method.
     *
     * @return the HTTP method
     */
    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Returns the request path.
     *
     * @return the path as a string
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the request body in JSON format.
     *
     * @return the JSON body string
     */
    public String getJsonBody() {
        return jsonBody;
    }
}