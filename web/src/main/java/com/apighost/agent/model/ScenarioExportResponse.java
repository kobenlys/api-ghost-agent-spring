package com.apighost.agent.model;

public class ScenarioExportResponse {

    private final boolean status;

    public ScenarioExportResponse(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

}
