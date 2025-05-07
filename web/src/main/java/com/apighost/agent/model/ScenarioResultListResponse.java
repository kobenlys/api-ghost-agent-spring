package com.apighost.agent.model;

import java.util.List;

public class ScenarioResultListResponse {

    private final List<ScenarioResultBrief> resultList;

    public ScenarioResultListResponse(List<ScenarioResultBrief> resultList) {
        this.resultList = resultList;
    }

    public List<ScenarioResultBrief> getResultList() {
        return resultList;
    }
}
