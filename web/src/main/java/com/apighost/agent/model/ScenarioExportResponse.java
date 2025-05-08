package com.apighost.agent.model;

/**
 * Represents the result of a scenario export operation.
 * <p>
 * Contains a simple status flag indicating whether the export succeeded or failed.
 * </p>
 *
 * @author oneweeeek
 * @version BETA-0.0.1
 */
public class ScenarioExportResponse {

    private final boolean status;

    /**
     * Constructs a {@code ScenarioExportResponse} with the given export status.
     *
     * @param status {@code true} if the export was successful, {@code false} otherwise
     */
    public ScenarioExportResponse(boolean status) {
        this.status = status;
    }

    /**
     * Returns the status of the export operation.
     *
     * @return {@code true} if successful, {@code false} otherwise
     */
    public boolean isStatus() {
        return status;
    }

}
