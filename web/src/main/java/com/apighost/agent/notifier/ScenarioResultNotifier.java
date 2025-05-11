package com.apighost.agent.notifier;

import com.apighost.model.scenario.ScenarioResult;
import com.apighost.model.scenario.result.ResultStep;

public interface ScenarioResultNotifier {

    void notifyStep(ResultStep step);

    void notifyCompletion(ScenarioResult result);

}
