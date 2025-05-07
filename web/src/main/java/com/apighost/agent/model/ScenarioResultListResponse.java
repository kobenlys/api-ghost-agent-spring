package com.apighost.agent.model;

import java.util.List;

/**
 * Represents a response containing a list of brief scenario test results.
 * <p>
 * This class is used to return multiple {@link ScenarioResultBrief} entries as a single response
 * object, typically for summary displays or API results.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ScenarioResultListResponse {

    private final List<ScenarioResultBrief> resultList;

    /**
     * Constructs a new {@code ScenarioResultListResponse} with the given list of scenario result
     * briefs.
     *
     * @param resultList the list of scenario result brief entries
     */
    public ScenarioResultListResponse(List<ScenarioResultBrief> resultList) {
        this.resultList = resultList;
    }

    public List<ScenarioResultBrief> getResultList() {
        return resultList;
    }
}
