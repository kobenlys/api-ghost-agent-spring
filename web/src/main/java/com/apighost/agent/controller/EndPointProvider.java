package com.apighost.agent.controller;

import com.apighost.agent.collector.RestApiCollector;
import com.apighost.agent.collector.WebSocketCollector;
import com.apighost.model.collector.Endpoint;
import java.util.ArrayList;
import java.util.List;
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

    private final RestApiCollector restApiCollector;
    private final WebSocketCollector webSocketCollector;
    /**
     * Initializes the {@code EndPointProvider} and triggers API endpoint scanning.
     *
     * @param restApiCollector the collector responsible for scanning available API endpoints
     */
    public EndPointProvider(RestApiCollector restApiCollector, WebSocketCollector webSocketCollector) {
        this.restApiCollector = restApiCollector;
        this.restApiCollector.scan();
        this.webSocketCollector = webSocketCollector;
        this.webSocketCollector.scan();
    }

    /**
     * Returns the list of collected API endpoints in JSON format.
     *
     * @return a {@link ResponseEntity} containing the endpoint list
     */
    @GetMapping("/endpoint-json")
    public ResponseEntity<?> getEndPoints() {
        List<Endpoint> endpointList = new ArrayList<>(restApiCollector.getEndpointList());
        endpointList.addAll(webSocketCollector.getEndpointList());
        return ResponseEntity.ok(endpointList);
    }

}
