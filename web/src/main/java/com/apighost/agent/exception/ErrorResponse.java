package com.apighost.agent.exception;

import com.apighost.util.file.TimeUtils;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
/**
 * Represents a standardized error response for exceptions in the application.
 *
 * <p>This class encapsulates error details including an error code, a descriptive message,
 * and a timestamp. It is typically constructed using the {@link Builder} pattern.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ErrorResponse response = new ErrorResponse.Builder()
 *     .code("E4001")
 *     .message("Invalid parameter.")
 *     .timestamp("2025-05-15T09:50:00Z")
 *     .build();
 * </pre>
 *
 * @author oneweeeek
 * @version BETA-0.0.1
 */
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String timestamp;

    /**
     * Constructs an ErrorResponse with the specified builder.
     *
     * @param builder the builder containing the error response details
     */
    private ErrorResponse(Builder builder){
        this.code = builder.code;
        this.message = builder.message;
        this.timestamp = builder.timestamp;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the timestamp of when the error occurred.
     *
     * @return the timestamp in ISO 8601 format
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Provides a builder for constructing {@link ErrorResponse} instances.
     *
     * <p>This builder supports JSON serialization via Jackson's {@link JsonPOJOBuilder} annotation.</p>
     *
     * @author oneweeeek
     * @version BETA-0.0.1
     */
    @JsonPOJOBuilder(
        withPrefix = ""
    )
    public static class Builder {

        private String code;
        private String message;
        private String timestamp;

        /**
         * Constructs a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the error code.
         *
         * @param code the error code
         * @return this builder instance
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param message the error message
         * @return this builder instance
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param timestamp the timestamp in ISO 8601 format
         * @return this builder instance
         */
        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds an {@link ErrorResponse} instance.
         *
         * <p>If the timestamp is not set, it defaults to the current time using
         * {@link TimeUtils#getNow()}.</p>
         *
         * @return the constructed ErrorResponse
         */
        public ErrorResponse build() {
            if (timestamp == null) {
                timestamp = TimeUtils.getNow();
            }
            return new ErrorResponse(this);
        }
    }
}
