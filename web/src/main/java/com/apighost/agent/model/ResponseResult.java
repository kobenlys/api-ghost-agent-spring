package com.apighost.agent.model;

import com.apighost.model.scenario.step.HTTPMethod;
import org.springframework.http.HttpStatusCode;

import java.util.Date;
import java.util.Map;

/**
 * Manages the response information of scenario tests (API requests) by encapsulating it into
 * objects.
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ResponseResult {

    private Map<String, Object> body;
    private HttpStatusCode httpStatus;
    private HTTPMethod httpMethod;
    private Map<String, String> header;
    private long startTime;
    private long endTime;
    private int durationMs;

    private ResponseResult(Builder builder) {
        this.body = builder.body;
        this.httpStatus = builder.httpStatus;
        this.httpMethod = builder.httpMethod;
        this.header = builder.header;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.durationMs = builder.durationMs;
    }

    public static class Builder {

        private Map<String, Object> body;
        private HttpStatusCode httpStatus;
        private HTTPMethod httpMethod;
        private Map<String, String> header;
        private long startTime;
        private long endTime;
        private int durationMs;

        public Builder body(Map<String, Object> body) {
            this.body = body;
            return this;
        }

        public Builder httpStatus(HttpStatusCode httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder httpMethod(HTTPMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder header(Map<String, String> header) {
            this.header = header;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder durationMs(int durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public ResponseResult build() {
            return new ResponseResult(this);
        }
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public HttpStatusCode getHttpStatus() {
        return httpStatus;
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getDurationMs() {
        return durationMs;
    }
}
