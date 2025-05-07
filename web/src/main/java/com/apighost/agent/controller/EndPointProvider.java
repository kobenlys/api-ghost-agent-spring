package com.apighost.agent.controller;

import com.apighost.agent.collector.ApiCollector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that provides API endpoint metadata collected from the application context.
 * <p>
 * Exposes a single endpoint to return all collected API mappings in JSON format.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
@RestController
@RequestMapping("/apighost")
public class EndPointProvider {

    private final ApiCollector apiCollector;

    /**
     * Initializes the {@code EndPointProvider} and triggers API endpoint scanning.
     *
     * @param apiCollector the collector responsible for scanning available API endpoints
     */
    public EndPointProvider(ApiCollector apiCollector) {
        this.apiCollector = apiCollector;
        this.apiCollector.scan();
    }

    /**
     * Returns the list of collected API endpoints in JSON format.
     *
     * @return a {@link ResponseEntity} containing the endpoint list
     */
    @GetMapping("/endpoint-json")
    public ResponseEntity<?> getEndPoints() {
        return ResponseEntity.ok(apiCollector.getEndPointList());
    }
}
