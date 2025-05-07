package com.apighost.agent.model;

import java.util.List;

/**
 * Response model that contains a list of available scenario file names.
 * <p>
 * This class is used to transfer scenario name data to API clients.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ScenarioListResponse {
    private List<String> scenarioNameList;

    /**
     * Constructs a new {@code ScenarioListResponse} with the given scenario name list.
     *
     * @param scenarioNameList the list of scenario file names
     */
    public ScenarioListResponse(List<String> scenarioNameList) {
        this.scenarioNameList = scenarioNameList;
    }

    public List<String> getScenarioNameList() {
        return scenarioNameList;
    }

    public void setScenarioNameList(List<String> scenarioNameList) {
        this.scenarioNameList = scenarioNameList;
    }
}
