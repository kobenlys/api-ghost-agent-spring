package com.apighost.agent.collector;

import com.apighost.model.collector.Endpoint;
import java.util.List;

public interface Collector {

    void scan();

    List<Endpoint> getEndpointList();
}
