package com.apighost.agent.config;

import com.apighost.agent.collector.ApiCollector;
import com.apighost.agent.controller.EndPointProvider;
import com.apighost.agent.controller.EngineController;
import com.apighost.agent.controller.ScenarioGUIController;
import com.apighost.agent.engine.FileLoaderEngine;
import com.apighost.agent.exception.GlobalExceptionHandler;
import com.apighost.agent.executor.HttpExecutor;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.file.FileExporter;
import com.apighost.agent.file.FileLoader;
import com.apighost.agent.file.ScenarioFileLoader;
import com.apighost.agent.orchestrator.ScenarioTestOrchestrator;
import com.apighost.orchestrator.OpenAiGenerateOrchestrator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

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
    public EndPointProvider endPointProvider(ApiCollector apiCollector) {
        // need core method
        return new EndPointProvider(apiCollector);
    }

    @Bean
    public ScenarioGUIController scenarioGUIController() {
        return new ScenarioGUIController();
    }

    @Bean
    public EngineController engineController(ScenarioTestOrchestrator scenarioTestOrchestrator,
        OpenAiGenerateOrchestrator openAiGenerateOrchestrator,
        FileLoaderEngine fileLoaderEngine, FileExporter fileExporter,
        ApiGhostSetting apiGhostSetting, ApiGhostProperties apiGhostProperties) {
        return new EngineController(scenarioTestOrchestrator, openAiGenerateOrchestrator,
            fileLoaderEngine, fileExporter,
            apiGhostSetting, apiGhostProperties);
    }

    @Bean
    public ApiCollector apiCollector(ApiGhostProperties apiGhostProperties) {
        return new ApiCollector(apiGhostProperties.getBasePackage(),
            apiGhostProperties.getBaseUrl());
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
    FileExporter fileExporter() {
        return new FileExporter();
    }

    @Bean
    public FileLoaderEngine fileLoaderEngine(FileLoader fileLoader,
        ApiGhostSetting apiGhostSetting) {
        return new FileLoaderEngine(fileLoader, apiGhostSetting);
    }

    @Bean
    public HttpExecutor httpExecutor() {
        return new HttpExecutor();
    }

    @Bean
    public ScenarioTestExecutor scenarioTestExecutor(ApiGhostSetting apiGhostSetting,
        ApiGhostProperties apiGhostProperties, HttpExecutor httpExecutor) {
        return new ScenarioTestExecutor(apiGhostSetting, apiGhostProperties, httpExecutor);
    }

    @Bean
    public ScenarioTestOrchestrator scenarioTestOrchestrator(ScenarioFileLoader scenarioFileLoader,
        ScenarioTestExecutor scenarioTestExecutor, FileExporter fileExporter,
        ApiGhostSetting apiGhostSetting) {
        return new ScenarioTestOrchestrator(scenarioFileLoader, scenarioTestExecutor, fileExporter,
            apiGhostSetting);
    }

    @Bean
    OpenAiGenerateOrchestrator openAiGenerateOrchestrator() {
        return new OpenAiGenerateOrchestrator();
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler(){
        return new GlobalExceptionHandler();
    }
}
