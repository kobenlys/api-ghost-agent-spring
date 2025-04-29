package com.apighost.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ScenarioGUIController {

    @GetMapping("/apighost-ui")
    public String showGUI() {
        return "forward:/apighost-ui.html";
    }
}
