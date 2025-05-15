package com.apighost.agent.exception;

import org.springframework.http.HttpStatus;
/**
 * Defines a set of error codes for standardized exception handling.
 *
 * <p>Each error code includes an HTTP status, a unique code, and a default message.
 * This enum is used to map exceptions to consistent error responses.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;
 * String code = errorCode.getCode(); // Returns "E4001"
 * String message = errorCode.getMessage(); // Returns "Invalid parameter."
 * </pre>
 *
 * @author oneweeeek
 * @version BETA-0.0.1
 */
public enum ErrorCode {

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "400", "Invalid parameter."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "400", "Invalid JSON format."),

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "The requested file was not found."),

    ILLEGAL_STATE(HttpStatus.CONFLICT, "409", "Invalid state change requested."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "An internal server error has occurred."),
    IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "An error occurred during an input/output operation."),
    CLASS_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "500", "The requested class was not found.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    /**
     * Constructs an ErrorCode with the specified HTTP status, code, and message.
     *
     * @param httpStatus the HTTP status associated with the error
     * @param code the unique error code
     * @param message the default error message
     */
    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    /**
     * Gets the HTTP status associated with the error.
     *
     * @return the HTTP status
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Gets the unique error code.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the default error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

}
