package com.apighost.agent.controller;

import com.apighost.agent.file.YamlHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ScenarioGUIController {

    private final YamlHandler yamlHandler;

    public ScenarioGUIController(){
        yamlHandler = new YamlHandler();
    }

    @GetMapping("/apighost-ui")
    public String showGUI() {
        return "forward:/apighost-ui.html";
    }

    @GetMapping("/apighost/import-yaml")
    public String importYamlFile(@RequestParam(value = "file") MultipartFile file){
        yamlHandler.yamlImporter(file);
        return "forward:/apighost-ui.html";
    }

}
