package com.apighost.agent.config;

import com.apighost.agent.collector.RestApiCollector;
import com.apighost.agent.collector.WebSocketCollector;
import com.apighost.agent.controller.EndPointProvider;
import com.apighost.agent.controller.EngineController;
import com.apighost.agent.controller.ScenarioGUIController;
import com.apighost.agent.engine.FileLoaderEngine;
import com.apighost.agent.exception.GlobalExceptionHandler;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.file.ScenarioFileLoader;
import com.apighost.agent.orchestrator.ScenarioTestOrchestrator;
import com.apighost.orchestrator.OpenAiGenerateOrchestrator;
import com.apighost.scenario.executor.HTTPStepExecutor;
import com.apighost.scenario.executor.StepExecutor;
import com.apighost.scenario.executor.WebSocketStepExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:apighost-settings.properties")
public class ApiGhostWebAutoConfiguration {

    @Bean
    public ApiGhostProperties apiGhostProperties(Environment env) {
        String basePackage = env.getProperty("apighost.lib.basePackage", "./");
        String baseUrl = env.getProperty("apighost.lib.baseUrl", "http://localhost:8080");
        String openAiKey = env.getProperty("apighost.lib.openAiKey", "");
        return new ApiGhostProperties(basePackage, baseUrl, openAiKey);
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
    public EndPointProvider endPointProvider(RestApiCollector restApiCollector, WebSocketCollector webSocketCollector) {
        // need core method
        return new EndPointProvider(restApiCollector, webSocketCollector);
    }

    @Bean
    public ScenarioGUIController scenarioGUIController() {
        return new ScenarioGUIController();
    }

    @Bean
    public EngineController engineController(ScenarioTestOrchestrator scenarioTestOrchestrator,
        OpenAiGenerateOrchestrator openAiGenerateOrchestrator, FileLoaderEngine fileLoaderEngine,
        ApiGhostSetting apiGhostSetting, ApiGhostProperties apiGhostProperties) {
        return new EngineController(scenarioTestOrchestrator, openAiGenerateOrchestrator,
            fileLoaderEngine,apiGhostSetting, apiGhostProperties);
    }

    @Bean
    public RestApiCollector apiCollector(ApiGhostProperties apiGhostProperties) {
        return new RestApiCollector(apiGhostProperties.getBasePackage(),
            apiGhostProperties.getBaseUrl());
    }

    @Bean
    public WebSocketCollector webSocketCollector(ApiGhostProperties apiGhostProperties) {
        return new WebSocketCollector(apiGhostProperties.getBasePackage(),
            apiGhostProperties.getBaseUrl());
    }

    @Bean
    public ScenarioFileLoader scenarioFileLoader() {
        return new ScenarioFileLoader();
    }

    @Bean
    public FileLoaderEngine fileLoaderEngine(ApiGhostSetting apiGhostSetting) {
        return new FileLoaderEngine(apiGhostSetting);
    }

    @Bean
    public ScenarioTestExecutor scenarioTestExecutor(ApiGhostSetting apiGhostSetting,
        ApiGhostProperties apiGhostProperties, @Qualifier("apighost-http-exe") StepExecutor http,
        @Qualifier("apighost-websocket-exe") StepExecutor websocket) {
        return new ScenarioTestExecutor(apiGhostSetting, apiGhostProperties, http, websocket);
    }

    @Bean("apighost-http-exe")
    public StepExecutor HttpStepExecutor() {
        return new HTTPStepExecutor();
    }

    @Bean("apighost-websocket-exe")
    public StepExecutor WebStocketStepExecutor() {
        return new WebSocketStepExecutor();
    }

    @Bean
    public ScenarioTestOrchestrator scenarioTestOrchestrator(ScenarioFileLoader scenarioFileLoader,
        ScenarioTestExecutor scenarioTestExecutor, ApiGhostSetting apiGhostSetting) {
        return new ScenarioTestOrchestrator(scenarioFileLoader, scenarioTestExecutor,
            apiGhostSetting);
    }

    @Bean
    OpenAiGenerateOrchestrator openAiGenerateOrchestrator() {
        return new OpenAiGenerateOrchestrator();
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
