package com.apighost.agent.config;

import com.apighost.agent.collector.ApiCollector;
import com.apighost.agent.controller.EndPointProvider;
import com.apighost.agent.controller.EngineController;
import com.apighost.agent.controller.ScenarioGUIController;
import com.apighost.agent.engine.FileLoaderEngine;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.loader.FileLoader;
import com.apighost.agent.loader.ScenarioFileLoader;
import com.apighost.parser.scenario.reader.JsonScenarioResultReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
@PropertySource("classpath:apighost-settings.properties")
public class ApiGhostWebAutoConfiguration {

    @Bean
    public ApiGhostProperties apiGhostProperties(Environment environment) {
        ApiGhostProperties properties = new ApiGhostProperties();
        String basePackage = environment.getProperty("apighost.lib.basePackage");
        properties.setBasePackage(basePackage);
        return properties;
    }

    @Bean
    public ApiGhostSetting apiGhostSetting(Environment env) {
        String scenarioPath = env.getProperty("apighost.base.scenarioPath");
        String resultPath = env.getProperty("apighost.base.resultPath");
        String formatYaml = env.getProperty("apighost.format.yaml");
        String formatYml = env.getProperty("apighost.format.yml");
        String formatJson = env.getProperty("apighost.format.json");
        return new ApiGhostSetting(scenarioPath, resultPath, formatYaml, formatYml, formatJson);
    }

    @Bean
    public EndPointProvider endPointProvider(ApiCollector apiCollector) {
        // need core method
        return new EndPointProvider(apiCollector);
    }

    @Bean
    public ScenarioGUIController scenarioGUIController() {
        return new ScenarioGUIController();
    }

    @Bean
    public EngineController engineController(ScenarioTestExecutor scenarioTestExecutor,
        FileLoaderEngine fileLoaderEngine) {
        return new EngineController(scenarioTestExecutor, fileLoaderEngine);
    }

    @Bean
    public ApiCollector apiCollector(ApiGhostProperties apiGhostProperties) {
        return new ApiCollector(apiGhostProperties.getBasePackage(), apiGhostProperties.getBaseUrl());
    }

    @Bean
    public RestTemplate restTemplate(ApplicationContext applicationContext) {
        Map<String, RestTemplate> restTemplateBeans = applicationContext.getBeansOfType(
            RestTemplate.class);
        if (restTemplateBeans.isEmpty()) {
            return new RestTemplate();
        }
        return restTemplateBeans.values().iterator().next();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ScenarioFileLoader scenarioFileLoader() {
        return new ScenarioFileLoader();
    }

    @Bean
    public FileLoader fileFinder(ApiGhostSetting apiGhostSetting) {
        return new FileLoader(apiGhostSetting);
    }

    @Bean
    public FileLoaderEngine fileLoaderEngine(FileLoader fileLoader,
        ObjectMapper objectMapper, ApiGhostSetting apiGhostSetting) {
        return new FileLoaderEngine(fileLoader, objectMapper, apiGhostSetting);
    }

    @Bean
    public ScenarioTestExecutor scenarioTestExecutor(RestTemplate restTemplate,
        ObjectMapper objectMapper, ScenarioFileLoader scenarioFileLoader,
        ApiGhostSetting apiGhostSetting) {
        return new ScenarioTestExecutor(restTemplate, objectMapper, scenarioFileLoader,
            apiGhostSetting);
    }

}
