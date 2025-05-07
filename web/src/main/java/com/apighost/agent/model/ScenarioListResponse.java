package com.apighost.agent.model;

import java.util.List;

public class ScenarioListResponse {
    private List<String> scenarioNameList;

    public List<String> getScenarioNameList() {
        return scenarioNameList;
    }

    public void setScenarioNameList(List<String> scenarioNameList) {
        this.scenarioNameList = scenarioNameList;
    }

    public ScenarioListResponse(List<String> scenarioNameList) {
        this.scenarioNameList = scenarioNameList;
    }
}
