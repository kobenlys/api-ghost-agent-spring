package com.apighost.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apighost")
public class EndPointProvider {

    @GetMapping("/endpoint-json")
    public ResponseEntity<?> getEndPoints() {
        // required core connect
        return ResponseEntity.ok("endpoint-list");
    }
}
