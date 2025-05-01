package com.apighost.agent.controller;

import com.apighost.agent.collector.ApiCollector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apighost")
public class EndPointProvider {

    private final ApiCollector apiCollector;

    public EndPointProvider(ApiCollector apiCollector) {
        this.apiCollector = apiCollector;
        this.apiCollector.scan();
    }

    @GetMapping("/endpoint-json")
    public ResponseEntity<?> getEndPoints() {
        return ResponseEntity.ok(apiCollector.getEndPointList());
    }
}
